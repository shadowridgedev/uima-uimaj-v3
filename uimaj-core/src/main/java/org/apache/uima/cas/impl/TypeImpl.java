/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.uima.cas.BuiltinTypeKinds;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.function.JCas_getter_boolean;
import org.apache.uima.cas.function.JCas_getter_byte;
import org.apache.uima.cas.function.JCas_getter_double;
import org.apache.uima.cas.function.JCas_getter_float;
import org.apache.uima.cas.function.JCas_getter_generic;
import org.apache.uima.cas.function.JCas_getter_int;
import org.apache.uima.cas.function.JCas_getter_long;
import org.apache.uima.cas.function.JCas_getter_short;
import org.apache.uima.cas.function.JCas_setter_boolean;
import org.apache.uima.cas.function.JCas_setter_byte;
import org.apache.uima.cas.function.JCas_setter_double;
import org.apache.uima.cas.function.JCas_setter_float;
import org.apache.uima.cas.function.JCas_setter_generic;
import org.apache.uima.cas.function.JCas_setter_int;
import org.apache.uima.cas.function.JCas_setter_long;
import org.apache.uima.cas.function.JCas_setter_short;
import org.apache.uima.util.Misc;

/**
 * The implementation of types in the type system.
 * 
 * UIMA Version 3

 * Instances of this class are not shared by different type systems because they contain a ref to the TypeSystemImpl (needed by FeaturePath and maybe other things)
 *   - even for built-ins.  
 *   - However, the JCas cover class definitions are shared by all type systems for built-in types
 * 
 * Feature offsets are set from the (changing) value of nbrOfIntDataFields and nbrOfRefDataFields
 * 
 */
public class TypeImpl implements Type, Comparable<TypeImpl> {  
        
  private final String name;                // x.y.Foo
  private final String shortName;           //     Foo
  
  private final int typeCode;               // subtypes always have typecodes > this one and < typeCodeNextSibling
  private       int depthFirstCode;         // assigned at commit time
  private       int depthFirstNextSibling;  // for quick subsumption testing, set at commit time

  private final TypeSystemImpl tsi ; // the Type System instance this type belongs to.
                                     // This means that built-in types have multiple instances, so this field can vary.
  
  /* the Java class for this type 
   *   integer = int.class, etc.
   *   used for args in methodType
   *   set when type is committed and JCas cover classes are loaded
   */
  protected       Class<?> javaClass;
  final protected Class<?> getter_funct_intfc_class;
  final protected Class<?> setter_funct_intfc_class;
  /* ***************** boolean flags *****************/
  protected boolean isFeatureFinal;

  protected boolean isInheritanceFinal;
  
  protected final boolean isLongOrDouble;  // for code generation
  
  /**
   * False for non creatable (as Feature Structures) values (e.g. byte, integer, string) and
   * also false for array built-ins (which can be Feature Structures, can be added-to-indexes, etc.)
   */
  protected final boolean isCreatableAndNotBuiltinArray;
  
  protected final boolean isRefType;  // not a primitive, can be a FeatureStructure in the CAS, added to indexes etc.
  
  
  /* ***************** type hierarchy *****************/
  private final TypeImpl superType;
  
  private final List<TypeImpl> allSuperTypes = new ArrayList<>();  
  
  private final List<TypeImpl> directSubtypes = new ArrayList<>();
    
  // ********  Features  *********
  private final Map<String, FeatureImpl> staticMergedFeatures = new HashMap<String, FeatureImpl>(1);
  private final List<FeatureImpl> staticMergedFeaturesIntroducedByThisType = new ArrayList<>(1);
  
  /**
   * The number of used slots needed = total number of features minus those represented by fields in JCas cover classes
   */
  int nbrOfUsedIntDataSlots = -1;
  int nbrOfUsedRefDataSlots = -1;
  
  // for journalling allocation: as above, but excluding the adjustment for fields in the JCas cover class
  int highestIntOffset = -1;  
  int highestRefOffset = -1;  
  
  /**
   * Create a new type. This should only be done by a <code>TypeSystemImpl</code>.
   */
  TypeImpl(String name, TypeSystemImpl tsi, final TypeImpl supertype) {
    this(name, tsi,supertype, FeatureStructureImplC.class);
  }
  
  TypeImpl(String name, TypeSystemImpl tsi, final TypeImpl supertype, Class<?> javaClass) {
        
    if (isStringSubtype() && supertype == tsi.stringType) {
      tsi.newTypeCheckNoInheritanceFinalCheck(name, supertype);  
    } else {
      tsi.newTypeChecks(name, supertype);
    }
    
    this.name = name;
    final int pos = this.name.lastIndexOf(TypeSystem.NAMESPACE_SEPARATOR);
    this.shortName = (pos >= 0) ? this.name.substring(pos + 1) : name;
    this.superType = supertype;
    
    this.isInheritanceFinal = false;
    this.isFeatureFinal = false;
    this.isLongOrDouble = name.equals(CAS.TYPE_NAME_LONG) || name.equals(CAS.TYPE_NAME_DOUBLE);
    this.tsi = tsi;
    this.typeCode = tsi.types.size();  // initialized with one null; so first typeCode == 1
    tsi.types.add(this);
    
    TypeImpl node = supertype;
    while (node != null) {
      allSuperTypes.add(node);
      node = node.superType;
    }
    
    if (null != this.superType) {  // top has null super
      if (!superType.isArray()) {
        // this because we have from V2: xyz[] is a subtype of FSArray, but FSArray doesn't list it as a direct sutype
        superType.directSubtypes.add(this);
      }
      staticMergedFeatures.putAll(superType.staticMergedFeatures);
    }
    this.isCreatableAndNotBuiltinArray = 
        // until stringType is set, skip this part of the test
        (tsi.stringType == null || supertype != tsi.stringType)  // string subtypes aren't FSs, they are only values   
        && !BuiltinTypeKinds.nonCreatableTypesAndBuiltinArrays_contains(name);
    
    this.isRefType = tsi.classifyAsRefType(name, supertype);
    this.javaClass = javaClass;
    tsi.typeName2TypeImpl.put(name, this);
    
    if (javaClass == boolean.class) {
      getter_funct_intfc_class = JCas_getter_boolean.class;
      setter_funct_intfc_class = JCas_setter_boolean.class;
    } else if (javaClass == byte.class) {
      getter_funct_intfc_class = JCas_getter_byte.class;
      setter_funct_intfc_class = JCas_setter_byte.class;
    } else if (javaClass == short.class) {
      getter_funct_intfc_class = JCas_getter_short.class;
      setter_funct_intfc_class = JCas_setter_short.class;
    } else if (javaClass == int.class) {
      getter_funct_intfc_class = JCas_getter_int.class;
      setter_funct_intfc_class = JCas_setter_int.class;
    } else if (javaClass == long.class) {
      getter_funct_intfc_class = JCas_getter_long.class;
      setter_funct_intfc_class = JCas_setter_long.class;
    } else if (javaClass == float.class) {
      getter_funct_intfc_class = JCas_getter_float.class;
      setter_funct_intfc_class = JCas_setter_float.class;
    } else if (javaClass == double.class) {
      getter_funct_intfc_class = JCas_getter_double.class;
      setter_funct_intfc_class = JCas_setter_double.class;
    } else {
      getter_funct_intfc_class = JCas_getter_generic.class;
      setter_funct_intfc_class = JCas_setter_generic.class;
    }
  }

  /**
   * Get the name of the type.
   * 
   * @return The name of the type.
   */
  @Override
  public String getName() {
    return this.name;
  }
  
  /**
   * Get the super type.
   * 
   * @return The super type or null for Top.
   */
  public Type getSuperType() {
    return this.superType;
  }

  /**
   * Return the internal integer code for this type. This is only useful if you want to work with
   * the low-level API.
   * 
   * @return The internal code for this type, <code>&gt;=0</code>.
   */
  public int getCode() {
    return this.typeCode;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getClass().getSimpleName() + " [name: ").append(name).append(", superType: ").append((superType == null) ? "<null>" :superType.getName()).append(", ");
    prettyPrintList(sb, "directSubtypes", directSubtypes, ti -> sb.append(ti.getName()));
    sb.append(", ");
    appendIntroFeats(sb);
    return sb.toString();
  }
  
  private <T> void prettyPrintList(StringBuilder sb, String title, List<T> items, Consumer<T> appender) {
    sb.append(title).append(": ");
    Misc.addElementsToStringBuilder(sb, items, appender);
  }
  
  private static final char[] blanks = new char[80];
  static {Arrays.fill(blanks,  ' ');}

  public void prettyPrint(StringBuilder sb, int indent) {
    sb.append(blanks, 0, Math.min(indent,  blanks.length)).append(name).append(": super: ").append(superType.getName());
    
    if (staticMergedFeaturesIntroducedByThisType.size() > 0) {
      sb.append(", ");
      appendIntroFeats(sb);
    }
    sb.append('\n');
  }
  
  public void prettyPrintWithSubTypes(StringBuilder sb, int indent) {
    prettyPrint(sb, indent);
    int nextIndent = indent + 2;
    directSubtypes.stream().forEachOrdered(ti -> prettyPrint(sb, nextIndent));
  }

  private void appendIntroFeats(StringBuilder sb) {
    prettyPrintList(sb, "FeaturesIntroduced/Range/multiRef", staticMergedFeaturesIntroducedByThisType,
        fi -> sb.append(fi.getShortName()).append('/')
                .append(fi.getRange().getName()).append('/')
                .append(fi.isMultipleReferencesAllowed() ? 'T' : 'F') );
  }

  /**
   * Get a vector of the features for which this type is the domain. Features will be returned in no
   * particular order.
   * 
   * @return The vector.
   * @deprecated
   */
  @Override
  @Deprecated
  public Vector<Feature> getAppropriateFeatures() {
    return new Vector<Feature>(getFeatures());

  }

  /**
   * Get the number of features for which this type defines the range.
   * 
   * @return The number of features.
   */
  @Override
  public int getNumberOfFeatures() {
    return staticMergedFeatures.size();
  }
  
  public boolean isAppropriateFeature(Feature feature) {
    return feature == staticMergedFeatures.get(feature.getShortName());
  }

  /**
   * Check if this is an annotation type.
   * 
   * @return <code>true</code>, if <code>this</code> is an annotation type; <code>false</code>,
   *         else.
   */
  public boolean isAnnotationType() {
    return false;
  }
  
  public boolean isAnnotationBaseType() {
    return false;
  }
  
  public boolean isCreatableAndNotBuiltinArray() {
    return isCreatableAndNotBuiltinArray;        
  }

  /**
   * Get the type hierarchy that this type belongs to.
   * 
   * @return The type hierarchy.
   */
  public TypeSystem getTypeSystem() {
    return this.tsi;
  }



  /**
   * Note: you can only compare types from the same type system. If you compare types from different
   * type systems, the result is undefined.
   */
  @Override
  public int compareTo(TypeImpl t) {
    if (this == t) {
      return 0;
    }

    return Integer.compare(System.identityHashCode(this), 
        System.identityHashCode(t));
  }

  /**
   * @see org.apache.uima.cas.Type#getFeatureByBaseName(String)
   */
  @Override
  public FeatureImpl getFeatureByBaseName(String featureShortName) {
    return staticMergedFeatures.get(featureShortName);
  }

  /**
   * @see org.apache.uima.cas.Type#getShortName()
   */
  @Override
  public String getShortName() {
    return this.shortName;
  }

  
  /**
   * @see org.apache.uima.cas.Type#isFeatureFinal()
   */
  @Override
  public boolean isFeatureFinal() {
    return this.isFeatureFinal;
  }

  /**
   * @see org.apache.uima.cas.Type#isInheritanceFinal()
   */
  @Override
  public boolean isInheritanceFinal() {
    return this.isInheritanceFinal;
  }

  void setFeatureFinal() {
    this.isFeatureFinal = true;
  }

  void setInheritanceFinal() {
    this.isInheritanceFinal = true;
  }
  
  public boolean isLongOrDouble() {
    return this.isLongOrDouble;
  }
  
  /**
   * @deprecated use getFeatureByBaseName instead
   * @param featureName -
   * @return -
   */
  @Deprecated 
  public Feature getFeature(String featureName) {
    return getFeatureByBaseName(featureName);
  }

  /**
   * guaranteed to be non-null, but might be empty list
   * @return -
   */
  @Override
  public List<Feature> getFeatures() {
    return new ArrayList<>(staticMergedFeatures.values());
  }
  
  Stream<FeatureImpl> getFeaturesAsStream() {
    return staticMergedFeatures.values().stream();
  }

  public List<FeatureImpl> getMergedStaticFeaturesIntroducedByThisType() {
    return staticMergedFeaturesIntroducedByThisType;
  }

  /**
   * @param fi feature to be added
   */
  void addFeature(FeatureImpl fi) {
    checkExistingFeatureCompatible(staticMergedFeatures.get(fi.getShortName()), fi.getRange());
    checkAndAdjustFeatureInSubtypes(this, fi);

    staticMergedFeatures.put(fi.getShortName(), fi);
    staticMergedFeaturesIntroducedByThisType.add(fi);
//    List<FeatureImpl> featuresSharingRange = getFeaturesSharingRange(fi.getRange());
//    if (featuresSharingRange == null) {
//      featuresSharingRange = new ArrayList<>();
//      range2AllFeaturesHavingThatRange.put((TypeImpl) fi.getRange(), featuresSharingRange);
//    }
//    featuresSharingRange.add(fi);
//    getAllSubtypes().forEach(ti -> ti.addFeature(fi));  // add the same feature to all subtypes
  }
  
  /**
   * It is possible that users may create type/subtype structure, and then add features (in any order) to that,
   * including adding a subtype feature "foo", and subsequently adding a type feature "foo".
   * 
   * To handle this:
   *   a feature added to type T should be 
   *     - removed if present in all subtype's introfeatures
   *     - added to all subtypes merged features
   *     - a check done in case any of the subtypes had already added this, but with a different definition
   * @param ti the type whose subtypes need checking
   * @param fi the feature
   */
  private void checkAndAdjustFeatureInSubtypes(TypeImpl ti, FeatureImpl fi) {
    String shortName = fi.getShortName();
    for (TypeImpl subti : ti.directSubtypes) {
      removeEqualFeatureNameMatch(subti.staticMergedFeaturesIntroducedByThisType, shortName);
      FeatureImpl existing = subti.staticMergedFeatures.get(shortName);
      checkExistingFeatureCompatible(existing, fi.getRange());
      if (existing == null) {
        subti.staticMergedFeatures.put(shortName, fi);
      }
      checkAndAdjustFeatureInSubtypes(subti, fi);
    }
  }

  private void removeEqualFeatureNameMatch(List<FeatureImpl> fiList, String name) {
    for (Iterator<FeatureImpl> it = fiList.iterator(); it.hasNext();) {
      FeatureImpl fi = it.next();
      if (fi.getShortName().equals(name)) {
        it.remove();
        break;
      }
    }
  }
  
  void checkExistingFeatureCompatible(FeatureImpl existingFi, Type range) {
    if (existingFi != null) {
      if (existingFi.getRange() != range) {
        /**
         * Trying to define feature "{0}" on type "{1}" with range "{2}", but feature has already been
         * defined on (super)type "{3}" with range "{4}".
         */
        throw new CASAdminException(CASAdminException.DUPLICATE_FEATURE, 
            existingFi            .getShortName(), 
            this                  .getName(), 
            range                 .getName(), 
            existingFi.getDomain().getName(), 
            existingFi.getRange() .getName());
      }
    }
  }
  
//  public String getJavaDescriptor() {
//    // built-ins
//    switch (typeCode) {
//    case TypeSystemImpl.booleanTypeCode: return "Z";
//    case TypeSystemImpl.byteTypeCode: return "B";
//    case TypeSystemImpl.shortTypeCode: return "S";
//    case TypeSystemImpl.intTypeCode: return "I";
//    case TypeSystemImpl.floatTypeCode: return "F";
//    case TypeSystemImpl.longTypeCode: return "J";
//    case TypeSystemImpl.doubleTypeCode: return "D";
//    case TypeSystemImpl.booleanArrayTypeCode: return "[Z";
//    case TypeSystemImpl.byteArrayTypeCode: return "[B";
//    case TypeSystemImpl.shortArrayTypeCode: return "[S";
//    case TypeSystemImpl.intArrayTypeCode: return "[I";
//    case TypeSystemImpl.floatArrayTypeCode: return "[F";
//    case TypeSystemImpl.longArrayTypeCode: return "[J";
//    case TypeSystemImpl.doubleArrayTypeCode: return "[D";
//    case TypeSystemImpl.stringArrayTypeCode: return "[Ljava/lang/String;";
//    }
//    
//    if (isStringSubtype()) {
//      return "Ljava/lang/String;";     
//    }
//    
//    return (isArray() ? "[" : "") + "L" + nameWithSlashes + ";";
//  }

  /**
   * Consolidate arrays of fsRefs to fsArrayType and ordinary fsRefs to TOP for generic getters and setters
   * @param topType -
   * @param fsArrayType - 
   * @return this type or one of the two passed in types
   */
  TypeImpl consolidateType(TypeImpl topType, TypeImpl fsArrayType) {
    if (!(isPrimitive())) {
      return topType;
    }
    // is one of the primitive (non-array) types
    return this;
  }

  /**
   * @see org.apache.uima.cas.Type#isPrimitive()
   */
  @Override
  public boolean isPrimitive() {
    return false;  // overridden by primitive typeimpl
  }

  /**
   * @see org.apache.uima.cas.Type#isArray()
   */
  @Override
  public boolean isArray() {
    return false;  // overridden by array subtype
  }
  
  /**
   * @see org.apache.uima.cas.Type#isStringSubtype()
   */
  @Override
  public boolean isStringSubtype() {
    return false;  // overridden by string subtype
  }

  @Override
  public Type getComponentType() {
    return null;  // not an array, array subtype overrides
  }

  /**
   * 
   * @return stream of all subtypes (excludes this type)
   *         in strict subsumption order
   */
  Stream<TypeImpl> getAllSubtypes() {
    return directSubtypes.stream().flatMap((TypeImpl ti) -> Stream.concat(Stream.of(ti), ti.getAllSubtypes())); 
  }
  
  List<TypeImpl> getDirectSubtypes() {
    return directSubtypes;
  }
  
  boolean hasSupertype(TypeImpl supertype) {
    return allSuperTypes.contains(supertype);
  }
    
//  public <T extends FeatureStructure> T createFS(CAS cas) {
//    if (null == creator) {
//      if (!tsi.isCommitted()) {
//        /** Can't create FS of type "{0}" until the type system has been committed. */
//        throw new CASRuntimeException(CASRuntimeException.CREATE_FS_BEFORE_TS_COMMITTED, getName());
//      }
//      generateJCasClass();  
//    }
//    return (T) creator.apply(cas);
//  }
//  
//  public TOP_Type create_Type(JCas jcas) {
//    return creator_type.create(jcas, this);
//  }
//  
//  /**
//   * Only called if creator is null, meaning the JCas Type and _Type classes haven't been generated yet.
//   * Not called for builtins with alternate names (because they're already there, by hand (not generated)
//   * 
//   * This only happens for lazy loaded situations, where there's a UIMATypeSystemClassLoader in the class loader chain.
//   *   For non-lazy situations (no UIMATypeSystemClassLoader), all the JCas classes were "batch" generated at commit time.
//   */
//  void generateJCasClass() {
//    TypeSystemImpl tsi = (TypeSystemImpl) this.getTypeSystem();
//    ClassLoader cl =tsi.get.Class().getClassLoader();    
//    // load and run static initializers, using the class loader of this TypeImpl
//    Class<?> jcasClass;
//    try {
//      jcasClass = Class.forName(name, true, cl);  // generate if not already loaded.  
//      // the _Type class is statically referenced from the other, and will be loaded too if needed. 
//      Method getAccessorsMethod = jcasClass.getMethod("__getAccessors");
//      Object[] aa = (Object[]) getAccessorsMethod.invoke(null);
//      this.creator = (Function<AbstractCas, TOP>) aa[0];
//      this.creator_type = (JCas_TypeCreator<?>) aa[1];
//      if (isAnnotationType()) {
//        ((TypeImplAnnot)this).creatorAnnot = (JCasAnnotCreator<?,?>) aa[2];
//      }
//      /**
//       * Run a parallel loop: the list of static features introduced by this type, and the rest of the accessors
//       *   one feature may have 2 or 4 accessors (get/set and optionally array-get/set)
//       */
//      final int nbrFeat = getNumberOfFeatures();
//      for (int acc_i = (isAnnotationType() ? 3 : 2), feat_i = 0; feat_i < nbrFeat; acc_i ++, feat_i ++) {
//        FeatureImpl fi = staticMergedFeaturesIntroducedByThisType.get(feat_i);
//        fi.setGetterMethodRef(aa[acc_i++]);
//        fi.setSetterMethodRef(aa[acc_i++]);
//        if (fi.getRange().isArray()) {
//          ((FeatureArrayImpl)fi).setGetterArrayMethodRef(aa[acc_i++]);
//          ((FeatureArrayImpl)fi).setSetterArrayMethodRef(aa[acc_i++]);  
//        }
//      }
//    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//      throw new UIMARuntimeException(e); // never happen
//    }   
//  }
//  
//  public void setCreator(Function<JCas, TOP> fi, Function<>) {
//    creator = fi;
//  }
      
  public boolean subsumes(TypeImpl ti) {
    
    if (depthFirstNextSibling != 0) {
      return (depthFirstCode <= ti.depthFirstCode && ti.depthFirstCode < depthFirstNextSibling);
    }
    return getTypeSystem().subsumes(this, ti);
  }
  
  public boolean subsumesValue(Object v) {
    return (v == null) ||
           (v instanceof String && typeCode == TypeSystemImpl.stringTypeCode) ||
           ((v instanceof FeatureStructureImplC) &&
             subsumes( ((FeatureStructureImplC)v)._typeImpl));
  }
  
  int computeDepthFirstCode(int level) {
    depthFirstCode = level ++;
    for (TypeImpl subti : directSubtypes) {
      level = subti.computeDepthFirstCode(level);
    }
    depthFirstNextSibling = level;
    return level;
  }
  
  /**
   * @return the javaClass
   */
  Class<?> getJavaClass() {
    return javaClass;
  }

  Class<?> getJavaPrimitiveClassOrObject() {
    if (!isPrimitive() || isStringSubtype() || 
        this == tsi.stringType) {
      return Object.class;
    }
    return javaClass;
  }
  
  /**
   * @param javaClass the javaClass to set
   */
  void setJavaClass(Class<?> javaClass) {
    this.javaClass = javaClass;
  }
  
}
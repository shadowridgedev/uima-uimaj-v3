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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.function.JCas_getter_boolean;
import org.apache.uima.cas.function.JCas_getter_double;
import org.apache.uima.cas.function.JCas_getter_generic;
import org.apache.uima.cas.function.JCas_getter_int;
import org.apache.uima.cas.function.JCas_getter_long;
import org.apache.uima.cas.function.JCas_setter_boolean;
import org.apache.uima.cas.function.JCas_setter_byte;
import org.apache.uima.cas.function.JCas_setter_float;
import org.apache.uima.cas.function.JCas_setter_generic;
import org.apache.uima.cas.function.JCas_setter_int;
import org.apache.uima.cas.function.JCas_setter_long;
import org.apache.uima.cas.function.JCas_setter_short;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.CommonArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.JavaObjectArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.impl.JCasImpl;
import org.apache.uima.util.Misc;

/**
 * Feature structure implementation (for non JCas and JCas)
 * 
 * Each FS has 
 *   - int data 
 *     - used for boolean, byte, short, int, long, float, double data
 *       -- long and double use 2 int slots
 *     - may be null if all slots are in JCas cover objects as fields
 *   - ref data
 *     - used for references to other Java objects, such as 
 *       -- strings
 *       -- other feature structures
 *       -- arbitrary Java Objects
 *     - may be null if all slots are in JCas cover objects as fields
 *   - an id: an incrementing integer, starting at 1, per CAS, of all FSs created for that CAS
 *   - a ref to the casView where this FS was created
 *   - a ref to the TypeImpl for this class
 *     -- can't be static - may be multiple type systems in use
 * 
 */
public class FeatureStructureImplC implements FeatureStructure, Cloneable {

  // note: these must be enabled to make the test cases work
  public static final String DISABLE_RUNTIME_FEATURE_VALIDATION = "uima.disable_runtime_feature_validation";
  public static final boolean IS_ENABLE_RUNTIME_FEATURE_VALIDATION  = !Misc.getNoValueSystemProperty(DISABLE_RUNTIME_FEATURE_VALIDATION);

  public static final String DISABLE_RUNTIME_FEATURE_VALUE_VALIDATION = "uima.disable_runtime_feature_validation";
  public static final boolean IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION  = !Misc.getNoValueSystemProperty(DISABLE_RUNTIME_FEATURE_VALUE_VALIDATION);

  // data storage
  // slots start with _ to prevent name collision with JCas style getters and setters.
  
  protected final int[] _intData;  
  protected final Object[] _refData;
  protected final int _id;  // a separate slot for access without loading _intData object

  
  /**
   * These next two object references are the same for every FS of this class created in one view.
   *   So, they could be stored in a shared object
   *     But that would trade off saving one "reference" for adding one extra load to get to the value
   *       This design uses more space instead.
   */
  
  /**
   * The view this Feature Structure was originally created in.
   * Feature Structures may be indexed in multiple views, or in no views.
   * 
   * Also used to access other metadata including the type system
   */
  protected final CASImpl _casView;  
  
  protected final TypeImpl _typeImpl;
  
  // Called only to generate a dummy value for the REMOVED flag in bag indexes

  public FeatureStructureImplC() {
    _casView = null;
    _typeImpl = null;
    _intData = null;
    _refData = null;
    _id = 0;    
  }

  
  /**
   * For non-JCas use
   * @param casView -
   * @param type -
   */
  protected FeatureStructureImplC(TypeImpl type, CASImpl casView) {
    _casView = casView;
    _typeImpl = type;
    
    int c = _typeImpl.nbrOfUsedIntDataSlots;
    _intData = (c == 0) ? null : new int[c];
    
    c = _typeImpl.nbrOfUsedRefDataSlots;
    _refData = (c == 0) ? null : new Object[c];
    
    _id = casView.setId2fs(this);    
  }

  /**
   * For JCas use (done this way to allow "final")
   * The TypeImpl is derived from the JCas cover class name
   * @param jcasImpl - the view this is being created in
   */
  
  protected FeatureStructureImplC(JCasImpl jcasImpl) {
    _casView = jcasImpl.getCasImpl();
    _typeImpl = _casView.getTypeSystemImpl().getJCasRegisteredType(getTypeIndexID());
    
    if (null == _typeImpl) {
      throw new CASRuntimeException(CASRuntimeException.JCAS_TYPE_NOT_IN_CAS, this.getClass().getName());
    }
    
    int c = _typeImpl.nbrOfUsedIntDataSlots;
    _intData = (c == 0) ? null : new int[c];
    
    c = _typeImpl.nbrOfUsedRefDataSlots;
    _refData = (c == 0) ? null : new Object[c];
    
    _id = _casView.setId2fs(this);    
  }
  
  
  // Only for clone use
  private FeatureStructureImplC(CASImpl casView, TypeImpl type, int[] intData, Object[] refData) {
    this._casView = casView;
    this._typeImpl = type;
    _intData = intData.clone();
    _refData = refData.clone();
    _id = casView.setId2fs(this);    
  }
  
  /* ***********************
   *    Index Add Remove
   * ***********************/
  
  /** add the corresponding FeatureStructure to all Cas indexes in the view where this FS was created*/
  public void addToIndexes() {
    _casView.addFsToIndexes(this);
  }
  
  /**
   * add this FS to indexes in a specific view, perhaps different from the creation view
   * @param jcas the JCas
   */
  public void addToIndexes(JCas jcas) {
    jcas.getCas().addFsToIndexes(this);
  }
  
  public void addToIndexes(CAS cas) {
    cas.addFsToIndexes(this);
  }
  

  /** remove the corresponding FeatureStructure from all Cas indexes in the view where this FS was created */
  public void removeFromIndexes() {
    removeFromIndexes(_casView);
  }

  /**
   * remove this FS from indexes in a specific view, perhaps different from the view where this was created.
   * @param cas the Cas
   */
  public void removeFromIndexes(CAS cas) {
    cas.removeFsFromIndexes(this);
  }

  
  /* *******************************
   *    IDs and Type
   *********************************/
  /**
   * NOTE: Possible name collision
   * @return the internal id of this fs - unique to this CAS, a positive int
   */
  public final int getAddress() { return _id; };

  @Override
  public final int get_id() {return _id; };
  
  // backwards compatibility
  @Override
  public Type getType() {
    return _typeImpl;
  }
  
  public int _getTypeCode() {
    return _typeImpl.getCode();
  }

  /* *********************************************************
   * Get and Set features indirectly, via Feature objects
   * 
   * There are two implementations, depending on whether or not
   * the feature has a JCas getter/setter.
   *   - If yes, then these just delegate to that (via a 
   *     functional interface stored in the Feature)
   *     -- there are multiple functional interfaces, corresponding
   *        to the all the different (primitive) return values:
   *        boolean, byte, short, int, long, float, double, and "Object"
   *          used for String and FeatureStructures
   *   - if no, then converge the code to an _intData or _refData reference
   ***********************************************************/
  protected void featureValidation(Feature feat) {
    if (!_typeImpl.isAppropriateFeature(feat)) {
      /* Feature "{0}" is not defined for type "{1}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT, feat.getName(), _typeImpl.getName());
    }
  }
  
  protected void featureValueValidation(Feature feat, Object v) {
    TypeImpl range = (TypeImpl)feat.getRange();
    if ((range.isArray() && !isOkArray(range, v)) ||
        (!range.isArray() && (!range.subsumesValue(v)))) {
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, feat.getName(), range.getName(), v.getClass().getName());
    }
  }
    
  // called when range isArray() is true, only
  private boolean isOkArray(TypeImpl range, Object v) {
    if (v == null) {
      return true;
    }
    
    final int rtc = range.getCode();

    /* The assignment is stricter than the Java rules - must match */
    switch (rtc) {
    case TypeSystemImpl.booleanArrayTypeCode:
      return v instanceof BooleanArray;
    case TypeSystemImpl.byteArrayTypeCode:
    return v instanceof ByteArray;
    case TypeSystemImpl.shortArrayTypeCode:
      return v instanceof ShortArray;
    case TypeSystemImpl.intArrayTypeCode:
      return v instanceof IntegerArray;
    case TypeSystemImpl.floatArrayTypeCode:
      return v instanceof FloatArray;
    case TypeSystemImpl.longArrayTypeCode:
      return v instanceof LongArray;
    case TypeSystemImpl.doubleArrayTypeCode:
      return v instanceof DoubleArray;
    case TypeSystemImpl.stringArrayTypeCode:
      return v instanceof StringArray;
    case TypeSystemImpl.javaObjectArrayTypeCode:
      return v instanceof JavaObjectArray;
    }
    
    if (!(v instanceof FeatureStructureImplC)) { return false; }
    final TypeImpl vType = ((FeatureStructureImplC) v)._typeImpl;
    if (!vType.isArray()) { return false; }
    
    if (rtc == TypeSystemImpl.fsArrayTypeCode) {
      return !vType.isPrimitive();
    }
    
    // because we cannot create xyz[] instances (10/2015) 
    // but can only create instances of FSArray
    // we violate the typing restrictions and allow
    // assigning FSArray  == TOP[] to some xyz[] type.
    // This should be fixed.
    final int vCode = vType.getCode();
    if (vCode == TypeSystemImpl.fsArrayTypeCode) {
      // range type isArray
      // range type is not one of the built-in primitive arrays
      // 
      // case where range type is TOP or ArrayBase is handled by 
      //   the caller
      return true;
    }
    
    // Both range and value are arrays, but 
    //   - neither are primitive arrays, and 
    //   - neither are fsArrays.
    
    // this case will only happen if we can create non FSArrays
    //   of particular types

    System.out.println("Debug - should never hit this");
    return false;
    
//    return (range.getComponentType() == ((TypeImpl)(vc._typeImpl)).getComponentType());
  }

  /**
   * Setters for values which could be keys in indexes have to do index corruption checking
   * 
   * All setters may have to journal which fs, feature (and for arrays, element) is being set.
   */

  protected void setIntValueCJ(FeatureImpl fi, int v) {
    if (!fi.isInInt) {
      /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}".*/
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(), "int", fi.getRange().getName());
    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(fi);
    _casView.setWithCheckAndJournal(this, fi.getCode(), () -> _intData[fi.getAdjustedOffset()] = v); 
  }

  protected void setRefValueCJ(FeatureImpl feat, Object v) {
    _casView.setWithCheckAndJournal(this, feat.getCode(), () -> _refData[feat.getAdjustedOffset()] = v); 
  }

  @Override
  public void setBooleanValue(Feature feat, boolean v) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_boolean)setter).set(this, v);
    } else {
      setIntValueCJ(fi, v ? 1 : 0); 
    }
  }
 
  @Override
  public void setByteValue(Feature feat, byte v) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_byte)setter).set(this, v);
    } else {
      setIntValueCJ(fi, v); 
    }
  }
  
  @Override
  public void setShortValue(Feature feat, short v) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_short)setter).set(this, v);
    } else {
      setIntValueCJ(fi, v);
    }
}

  @Override
  public void setIntValue(Feature feat, int v) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_int)setter).set(this, v);
    } else {
      setIntValueCJ(fi, v);
    }
  }
  
  @Override
  public void setLongValue(Feature feat, long v) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_long)setter).set(this, v);
    } else {
      if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
      _casView.setFeatureValue(this, (FeatureImpl) feat, (int)(v & 0xffffffff), (int)(v >> 32));
    }
  }

  @Override
  public void setFloatValue(Feature feat, float v) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_float)setter).set(this, v);
    } else {
      setIntValueCJ(fi, CASImpl.float2int(v));
    }
  }

  @Override
  public void setDoubleValue(Feature feat, double v) {
    setLongValue(feat, CASImpl.double2long(v));}

  @Override
  public void setStringValue(Feature feat, String v) {
    TypeImpl range = (TypeImpl) feat.getRange();
    if (range.isStringSubtype()) {
      if (v != null) {
        TypeImplStringSubtype tiSubtype = (TypeImplStringSubtype) range;
        tiSubtype.validateIsInAllowedValues(v);
      }
    } else if (range.getCode() != TypeSystemImpl.stringTypeCode) {
      /** Expected value of type "{0}", but found "{1}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE, range.getName(), "Java String");
    }
    FeatureImpl fi = (FeatureImpl) feat;
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_generic<String>)setter).set(this, v);
    } else {
      setRefValueCJ(fi, v);
    }
  }
  

  @Override
  public void setFeatureValue(Feature feat, FeatureStructure v) {
    FeatureImpl fi = (FeatureImpl) feat;
 
    if (fi.isInInt) {
      /** Trying to access value of feature "{0}" as feature structure, but is primitive type. */
      throw new CASRuntimeException(CASRuntimeException.PRIMITIVE_VAL_FEAT, feat.getName());
    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    if (IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION) featureValueValidation(feat, v);
 
    Object setter =  fi.getJCasSetter();
    if (setter != null) {
      ((JCas_setter_generic<FeatureStructureImplC>)setter).set(this, (FeatureStructureImplC) v);
    } else {
      setRefValueCJ(fi, v); 
    }
  }

  @Override
  public void setJavaObjectValue(Feature feat, Object v) { 
    if (v instanceof String) {
      setStringValue(feat,  (String) v);  // in order to do proper string subtype checking
    } else { 
      FeatureImpl fi = (FeatureImpl) feat;
      
      if (fi.isInInt) {
        /** Trying to access value of feature "{0}" as feature structure, but is primitive type. */
        throw new CASRuntimeException(CASRuntimeException.PRIMITIVE_VAL_FEAT, feat.getName());
      }
      if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
      if (IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION) featureValueValidation(feat, v);

      Object setter =  fi.getJCasSetter();
      if (setter != null) {
        ((JCas_setter_generic<Object>)setter).set(this, v);
      } else {
        final int adjustedOffset = ((FeatureImpl)feat).getAdjustedOffset();
        if (-1 == adjustedOffset) {
          /** JCas Class "{0}" is missing required field accessor, or access not permitted, for field "{1}" during {2} operation. */
          throw new CASRuntimeException(CASRuntimeException.JCAS_MISSING_FIELD_ACCESSOR, 
              fi.getHighestDefiningType().javaClass.getName(),
              fi.getShortName(), 
              "set");
        }
        setRefValueCJ(fi, v);
      }
    }
  }

  @Override
  public void setFeatureValueFromString(Feature feat, String s) throws CASRuntimeException {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    _casView.setFeatureValueFromString(this, (FeatureImpl) feat, s);
  }
  
  /**   G E T T E R S **/

  @Override
  public boolean getBooleanValue(Feature feat) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object getter =  fi.getJCasGetter();
    return (getter != null) ? ((JCas_getter_boolean)getter).get(this)
                            : getIntValue(feat) == 1;
  }

  @Override
  public byte getByteValue(Feature feat) { return (byte) getIntValue(feat); }

  @Override
  public short getShortValue(Feature feat) { return (short) getIntValue(feat); }

  @Override
  public int getIntValue(Feature feat) {
    return getIntValue((FeatureImpl)feat);
  }
    
  public int getIntValue(FeatureImpl feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    Object getter =  feat.getJCasGetter();
    return (getter != null) ? ((JCas_getter_int)getter).get(this)
                            : _intData[feat.getAdjustedOffset()]; 
  }

  @Override
  public long getLongValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    FeatureImpl fi = (FeatureImpl) feat;
    Object getter =  fi.getJCasGetter();
    return (getter != null) ? ((JCas_getter_long)getter).get(this)
                            : getLongValueOffset(fi.getAdjustedOffset());
  }
  
  /**
   * When converting the lower 32 bits to a long, sign extension is done, so have to 
   * 0 out those bits before or-ing in the high order 32 bits.
   * @param offset -
   * @return -
   */
  public long getLongValueOffset(int offset) {
    return (((long)_intData[offset]) & 0x00000000ffffffffL) | (((long)_intData[offset + 1]) << 32);      
  }

  @Override
  public float getFloatValue(Feature feat) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object getter =  fi.getJCasGetter();
    return (getter != null) ? ((JCas_getter_long)getter).get(this)
                            : (float) CASImpl.int2float(getIntValue(feat)); 
  }
  
  @Override
  public double getDoubleValue(Feature feat) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object getter =  fi.getJCasGetter();
    return (getter != null) ? ((JCas_getter_double)getter).get(this)
                            : CASImpl.long2double(getLongValue(feat)); }
  
  public double getDoubleValueOffset(int offset) {
    return CASImpl.long2double(getLongValueOffset(offset));
  }
  
  @Override
  public String getStringValue(Feature feat) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object getter =  fi.getJCasGetter();
    return (getter != null) ? ((JCas_getter_generic<String>)getter).get(this)
                            : (String) getJavaObjectValue(feat);
  }

  @Override
  public FeatureStructure getFeatureValue(Feature feat) {
    FeatureImpl fi = (FeatureImpl) feat;
    Object getter =  fi.getJCasSetter();
    return (getter != null) ? ((JCas_getter_generic<FeatureStructure>)getter).get(this)
                            : (FeatureStructure) getJavaObjectValue(feat);
  }

  @Override
  public Object getJavaObjectValue(Feature feat) { 
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    FeatureImpl fi = (FeatureImpl) feat;
    Object getter =  fi.getJCasGetter();
    if (getter == null) {
      final int adjustedOffset = ((FeatureImpl)feat).getAdjustedOffset();
      if (-1 == adjustedOffset) {
        /** JCas Class "{0}" is missing required field accessor, or access not permitted, for field "{1}" during {2} operation. */
        throw new CASRuntimeException(CASRuntimeException.JCAS_MISSING_FIELD_ACCESSOR, 
            fi.getHighestDefiningType().javaClass.getName(),
            fi.getShortName(), 
            "get");
      }
      return _refData[((FeatureImpl)feat).getAdjustedOffset()];
    }
    return ((JCas_getter_generic<Object>)getter).get(this);
  }

  @Override
  public String getFeatureValueAsString(Feature feat) throws CASRuntimeException {
    return _casView.getFeatureValueAsString(this, (FeatureImpl) feat);
  }

  /**
   * @return the CAS view where this FS was created
   */
  @Override
  public CAS getCAS() {
    return this._casView;
  }

  protected CASImpl _casView() { // was package private 9-03
    return this._casView;
  }
  
  /**
   * See http://www.javaworld.com/article/2076332/java-se/how-to-avoid-traps-and-correctly-override-methods-from-java-lang-object.html
   * for suggestions on avoiding bugs in implementing clone
   * 
   * Because we have final fields for _intData, _refData, and _id, we can't use clone.
   * Instead, we use the createFS to create the FS of the right type.  This will use the generators.
   * 
   * Strategy for cloning:
   *   Goal is to create an independent instance of some subtype of this class, with 
   *   all the fields properly copied from this instance.
   *     - some fields could be in the _intData and _refData
   *     - some fields could be stored as features
   *     
   * Subcases to handle:
   *   - arrays - these have no features.
   *   
   * @return a new Feature Structure as a new instance of the same class, 
   *         with a new _id field, 
   *         with its features set to the values of the features in this Feature Structure
   * @throws CASRuntimeException (different from Object.clone()) if an exception occurs   
   */
  @Override
  public FeatureStructureImplC clone() throws CASRuntimeException { 
        
    if (_typeImpl.isArray()) {
      CommonArray original = (CommonArray) this;
      CommonArray copy = _casView.createArray(_typeImpl.getCode(), original.size());
      copy.copyValuesFrom(original);      
      return copy;
    }
    
    FeatureStructureImplC fs = _casView.createFS(_typeImpl);
    
    final int sofaFeatCode = TypeSystemImpl.annotSofaFeatCode;

    /* copy all the feature values except the sofa ref which is already set as part of creation */
    for (Feature feat : _typeImpl.getFeatures()) {
      final FeatureImpl fi = (FeatureImpl) feat;
      if (fi.getCode() == sofaFeatCode) continue;
        
      switch (fi.getRangeImpl().getCode()) {
      case TypeSystemImpl.booleanTypeCode : fs.setBooleanValue(feat, getBooleanValue(feat)); break;
      case TypeSystemImpl.byteTypeCode : fs.setByteValue(feat, getByteValue(feat)); break;
      case TypeSystemImpl.shortTypeCode : fs.setShortValue(feat, getShortValue(feat)); break;
      case TypeSystemImpl.intTypeCode : fs.setIntValue(feat, getIntValue(feat)); break;
      case TypeSystemImpl.longTypeCode : fs.setLongValue(feat, getLongValue(feat)); break;
      case TypeSystemImpl.floatTypeCode : fs.setFloatValue(feat, getFloatValue(feat)); break;
      case TypeSystemImpl.doubleTypeCode : fs.setDoubleValue(feat, getDoubleValue(feat)); break;
      case TypeSystemImpl.stringTypeCode : fs.setStringValue(feat, getStringValue(feat)); break;
      default:  // for javaObject and fs ref
        if (fi.getRangeImpl().isRefType) {
          fs.setFeatureValue(feat, getFeatureValue(feat));
        } else {
          fs.setJavaObjectValue(feat, getJavaObjectValue(feat));
        }
      } // end of switch
    }   // end of for loop
    return fs;
  }

  @Override
  public int hashCode() {
    return _id;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Pretty printing.

  private static class PrintReferences {

    static final int NO_LABEL = 0;

    static final int WITH_LABEL = 1;

    static final int JUST_LABEL = 2;

    private static final String refNamePrefix = "#";

    // map from fs to special string #nnnn for printing refs
    // three states:
    //    1) key not in map
    //    2) key in map, but value is "seen once"  - first time value seen
    //    3) key in map, value is #nnnn - when value is seen more than once
    private Map<FeatureStructure, String> tree = new HashMap<FeatureStructure, String>();

    private Set<FeatureStructure> seen = new HashSet<FeatureStructure>();

    private int count;

    private PrintReferences() {
      super();
      this.count = 0;
    }

    /**
     * @param fs -
     * @return true if seen before
     */
    boolean addReference(FeatureStructure fs) {
      String v = tree.get(fs);
      if (null == v) {
        tree.put(fs, "seen once");
        return false;
      }
      if (v.equals("seen once")) {
        tree.put(fs, refNamePrefix + Integer.toString(this.count++));
      }
      return true;
    }

    String getLabel(FeatureStructure ref) {
      return this.tree.get(ref);
    }

    int printInfo(FeatureStructure ref) {
      if (this.tree.get(ref) == null) {
        return NO_LABEL;
      }
      if (this.seen.contains(ref)) {
        return JUST_LABEL;
      }
      this.seen.add(ref);
      return WITH_LABEL;
    }

  }

  private final void getPrintRefs(PrintReferences printRefs) {
    getPrintRefs(printRefs, this);
  }

  private final void getPrintRefs(PrintReferences printRefs, FeatureStructure fs) {
    boolean seenBefore = printRefs.addReference(fs);
    if (seenBefore) {
      return;
    }
    
    final TypeImpl ti = this._typeImpl;
    ti.getFeaturesAsStream()
        .filter(fi -> fi.getRangeImpl().isRefType)     // is ref type
        .map(fi -> this.getFeatureValue(fi)) // get the feature value
        .filter(refFs -> refFs != null)            // skip null ones
        .forEachOrdered(refFs -> getPrintRefs(printRefs, refFs));
  }

  @Override
  public String toString() {
    return toString(3);
  }

  public String toString(int indent) {
    StringBuilder buf = new StringBuilder();
    prettyPrint(0, indent, buf, true, null);
    return buf.toString();
  }

  public void prettyPrint(int indent, int incr, StringBuilder buf, boolean useShortNames) {
    prettyPrint(indent, incr, buf, useShortNames, null);
  }

  public void prettyPrint(int indent, int incr, StringBuilder buf, boolean useShortNames, String s) {
    PrintReferences printRefs = new PrintReferences();
    getPrintRefs(printRefs);
    prettyPrint(indent, incr, buf, useShortNames, s, printRefs);
  }

  public void prettyPrint(
      int indent, 
      int incr, 
      StringBuilder buf, 
      boolean useShortNames, 
      String s, 
      PrintReferences printRefs) {
    indent += incr;
    final int printInfo = printRefs.printInfo(this);
    if (printInfo != PrintReferences.NO_LABEL) {
      String label = printRefs.getLabel(this);
      if (!label.equals("seen once")) {
        buf.append(printRefs.getLabel(this));
      }
      if (printInfo == PrintReferences.JUST_LABEL) {
        buf.append('\n');
        return;
      }
      buf.append(' ');
    }
    if (useShortNames) {
      buf.append(getType().getShortName());
    } else {
      buf.append(getType().getName());
    }
    buf.append(':').append(_id);
    if (s != null) {
      buf.append(" \"" + s + "\"");
    }
    buf.append('\n');

//    final int typeClass = this._casView.ll_getTypeClass(this.getType());
    
    
    switch (_getTypeCode()) {
    case TypeSystemImpl.stringArrayTypeCode: {
      StringArray a = (StringArray) this;
      printArrayElements(a.size(), a::get, indent, buf);
      return;
    }
    case TypeSystemImpl.intArrayTypeCode: {
      IntegerArray a = (IntegerArray) this;
      printArrayElements(a.size(), i -> Integer.toString(a.get(i)), indent, buf);
      return;
    }
    case TypeSystemImpl.floatArrayTypeCode: {
      FloatArray a = (FloatArray) this;
      printArrayElements(a.size(), i -> Float.toString(a.get(i)), indent, buf);
      return;
    }
    case TypeSystemImpl.booleanArrayTypeCode: {
      BooleanArray a = (BooleanArray) this;
      printArrayElements(a.size(), i -> Boolean.toString(a.get(i)), indent, buf);
      return;
    }
    case TypeSystemImpl.byteArrayTypeCode: {
      ByteArray a = (ByteArray) this;
      printArrayElements(a.size(), i -> Byte.toString(a.get(i)), indent, buf);
      return;
    }
    case TypeSystemImpl.shortArrayTypeCode: {
      ShortArray a = (ShortArray) this;
      printArrayElements(a.size(), i -> Short.toString(a.get(i)), indent, buf);
      return;
    }
    case TypeSystemImpl.longArrayTypeCode: {
      LongArray a = (LongArray) this;
      printArrayElements(a.size(), i -> Long.toString(a.get(i)), indent, buf);
      return;
    }
    case TypeSystemImpl.doubleArrayTypeCode: {
      DoubleArray a = (DoubleArray) this;
      printArrayElements(a.size(), i -> Double.toString(a.get(i)), indent, buf);
      return;
    }
    }    
    
    for (Feature feat : _typeImpl.getFeatures()) {
      FeatureImpl fi = (FeatureImpl) feat;
      StringUtils.printSpaces(indent, buf);
      buf.append(fi.getShortName() + ": ");
      TypeImpl range = (TypeImpl) fi.getRange();
      
      if (range.getCode() == TypeSystemImpl.stringTypeCode ||
          range.isStringSubtype()) {
        String stringVal = getStringValue(fi);
        stringVal = (null == stringVal) ? "<null>" : "\"" + stringVal + "\"";
        buf.append(stringVal + '\n');
        continue;
      }
      
      if (!range.isPrimitive()) {   
        // not primitive
        FeatureStructure val = null;
        boolean hadException = false;
        try {
          val = getFeatureValue(fi);
        } catch (Exception e) {
          buf.append("<exception ").append(e.getMessage()).append(">\n");
          hadException = true;
        }
        if (!hadException) {
          if (val != null && !feat.getName().equals(CAS.TYPE_NAME_SOFA)) {
            ((FeatureStructureImplC) val).prettyPrint(indent, incr, buf, useShortNames, null, printRefs);
          } else {
            buf.append((val == null) ? "<null>\n" : ((SofaFS) val).getSofaID() + '\n'); 
          }
        }
    
      } else {  
        // is primitive
        buf.append(this.getFeatureValueAsString(feat) + "\n");
      }
    }
  }

  private void printArrayElements(int arrayLen, IntFunction<String> f, int indent, StringBuilder buf) {
    StringUtils.printSpaces(indent, buf);
    buf.append("Array length: " + arrayLen + "\n");
    if (arrayLen > 0) {
      StringUtils.printSpaces(indent, buf);
      buf.append("Array elements: [");
      int numToPrint = Math.min(15, arrayLen);  // print 15 or fewer elements

      for (int i = 0; i < numToPrint; i++) {
        if (i > 0) {
          buf.append(", ");
        }
        String element = f.apply(i); //this._casView.ll_getStringArrayValue(this.getAddress(), i);
        buf.append("\"" + element + "\"");
      }
      
      if (arrayLen > numToPrint) {
        buf.append(", ...");
      }
      buf.append("]\n");
    }
  }
  
  public int getTypeIndexID() {
    throw new CASRuntimeException(CASRuntimeException.INTERNAL_ERROR); // dummy, always overridden
  }
  
}
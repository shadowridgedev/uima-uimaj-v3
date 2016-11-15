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

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.BuiltinTypeKinds;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * There is one **class** instance of this per UIMA core class loader.
 *   The class loader is the loader for the UIMA core classes, not any UIMA extension class loader
 *   - **Builtin** JCas Types are loaded and shared among all type systems, once, when this class is loaded.
 * 
 * There are no instances of this class.  
 *   - The type system impl instances at commit time initialize parts of their Impl from data in this class
 *   - Some of the data kept in this class in static values, is constructed when the type system is committed
 * 
 * The class instance is shared
 *   - by multiple type systems 
 *   - by multiple CASes (in a CAS pool, for instance, when these CASes are sharing the same type system).
 *   - by all views of those CASes.
 *   - by multiple different pipelines, built using the same merged type system instance
 *   - by non-built-in JCas classes, loaded under possibly different extension class loaders
 *   
 * PEAR support
 *   Multiple PEAR contexts can be used.
 *   - hierarchy (each is parent of kind below
 *     -- UIMA core class loader (built-ins, not redefinable by user JCas classes) 
 *         --- a new limitation of UIMA V3 to allow sharing of built-in JCas classes, which also
 *             have custom impl, and don't fit the model used for PEAR Trampolines
 *     -- outer (non Pear) class loader (optional, known as base extension class loader)
 *         --- possible multiple, for different AE pipelines
 *     -- Within PEAR class loader
 *   - when running within a PEAR, operations which return Feature Structures potentially return
 *     JCas instances of classes loaded from the Pear's class loader. 
 *       - These instances share the same int[] and Object[] and _typeImpl and _casView refs with the outer class loader's FS
 * 
 * Timing / life cycle
 *   Built-in classes loaded and initialized at first type system commit time.
 *   non-pear classes loaded and initialized at type system commit time (if not already loaded)
 *     - special checks for conformability if some types loaded later, due to requirements for computing feature offsets at load time
 *   pear classes loaded and initialized at first entry to Pear, for a given type system and class loader.        
 *
 *          
 *   At typeSystemCommit time, this class is created and initialized: 
 *     - The built-in JCas types are loaded
 *     
 *     - The user-defined non-PEAR JCas classes are loaded (not lazy, but eager), provided the type system is a new one.
 *       (If the type system is "equal" to an existing committed one, that one is used instead).
 *          
 *       -- User classes defined with the name of UIMA types, but which are not JCas definitions, are not used as 
 *          JCas types.  This permits uses cases where users define a class which (perhaps at a later integration time)
 *          has the same name as a UIMA type, but is not a JCas class.
 *       -- These classes, once loaded, remain loaded because of Java's design, unless the ClassLoader
 *          used to load them is Garbage Collected.
 *          --- The ClassLoader used is the CAS's JCasClassLoader, set from the UIMA Extension class loader if specified.
 *              
 *   Assigning slots for features:
 *     - each type being loaded runs static final initializers to set for (a subset of) all features the offset
 *       in the int or ref storage arrays for those values. 
 *     - These call a static method in JCasRegistry: register[Int/Ref]Feature, which assigns the next available slot
 *       via accessing/updating a thread local instance of TypeSystemImpl.SlotAllocate.
 */

public abstract class FSClassRegistry { // abstract to prevent instantiating; this class only has static methods
      
  // Used for the built-ins.
  private static final MethodHandles.Lookup defaultLookup = MethodHandles.lookup();
  
  private static MethodHandles.Lookup lookup;
  
  private static final MethodType findConstructorJCasCoverType      = methodType(void.class, TypeImpl.class, CASImpl.class);
//  private static final MethodType findConstructorJCasCoverTypeArray = methodType(void.class, TypeImpl.class, CASImpl.class, int.class);
  /**
   * The callsite has the return type, followed by capture arguments
   */
  private static final MethodType callsiteFsGenerator      = methodType(FsGenerator.class);
//  private static final MethodType callsiteFsGeneratorArray = methodType(FsGeneratorArray.class);  // NO LONGER USED
  
  private static final MethodType fsGeneratorType      = methodType(TOP.class, TypeImpl.class, CASImpl.class);
//  private static final MethodType fsGeneratorArrayType = methodType(TOP.class, TypeImpl.class, CASImpl.class, int.class); // NO LONGER USED

  static private class ErrorReport {
    final Exception e;
    final boolean doThrow;
    ErrorReport(Exception e, boolean doThrow) {
      this.e = e;
      this.doThrow = doThrow;
    }
  }
  // must precede first (static) use
  static private ThreadLocal<List<ErrorReport>> errorSet = new ThreadLocal<>();
 
  /**
   * One instance per JCas Class per class loader
   * 
   * Created when potentially loading JCas classes.
   * 
   * Entries kept in potentially multiple global static hashmap, typename (string)
   *   - one hashmap per classloader
   *   Entries reused potentially by multiple type systems.
   * 
   * Info used for identifying the target of a "new" operator - could be generator for superclass.
   * One entry per defined JCas class per classloader; no instance if no JCas class is defined.
   */
  public static class JCasClassInfo {
    
    final FsGenerator generator;
   
    /**
     * The corresponding loaded JCas Class
     */
    final Class<?> jcasClass;
    
    /**
     * NOT the TypeCode, but instead, the unique int assigned to the JCas class 
     * when it gets loaded
     * Might be -1 if the JCasClassInfo instance is for a non-JCas instantiable type
     */
    final int jcasType;
        
    JCasClassInfo(Class<?> jcasClass, FsGenerator generator, int jcasType) {
      this.generator = generator;
      this.jcasClass = jcasClass;
      this.jcasType = jcasType;    // typeId for jcas class, **NOT Typecode**
      
//      System.out.println("debug create jcci, class = " + jcasClass.getName() + ", typeint = " + jcasType);
    }
    
    boolean isCopydown(TypeImpl ti) {
      return isCopydown(Misc.typeName2ClassName(ti.getName()));
    }
  
    boolean isCopydown(String jcasClassName) {
      return !jcasClass.getCanonicalName().equals(jcasClassName);
    }
    
    boolean isPearOverride(ClassLoader cl) {
      return jcasClass.getClassLoader().equals(cl);
    }
  }

  /**
   * Map from class loaders used to load JCas Classes, both PEAR and non-Pear cases, to JCasClassInfo for that loaded JCas class instance.
   *   Key is JCas fully qualified name (not UIMA type name).
   *   Is String, since different type systems may use the same JCas classes.
   * Cache of FsGenerator[]s kept in TypeSystemImpl instance, since it depends on type codes.
   * Current FsGenerator[] kept in CASImpl shared view data, switched as needed for PEARs. 
   */
  private static final Map<ClassLoader, Map<String, JCasClassInfo>> cl2type2JCas = new IdentityHashMap<>();
    
  /**
   * precomputed generators for built-in types
   * These instances are shared for all type systems
   * Key = index = typecode
   */
  private static final JCasClassInfo[] jcasClassesInfoForBuiltins;

  static {
    TypeSystemImpl tsi = TypeSystemImpl.staticTsi;
    jcasClassesInfoForBuiltins = new JCasClassInfo[tsi.getTypeArraySize()]; 
    lookup = defaultLookup;
        
    // walk in subsumption order, supertype before subtype
    // Class loader used for builtins is the UIMA framework's class loader
    loadBuiltins(tsi.topType, tsi.getClass().getClassLoader());
    
    reportErrors();
  }
  
  private static void loadBuiltins(TypeImpl ti, ClassLoader cl) {
    String typeName = ti.getName();
    
    if (BuiltinTypeKinds.creatableBuiltinJCas.contains(typeName) || typeName.equals(CAS.TYPE_NAME_SOFA)) {
      Class<?> builtinClass = maybeLoadJCas(ti, cl);
      assert (builtinClass != null);  // builtin types must be present
      // copy down to subtypes, if needed, done later
      int jcasType = Misc.getStaticIntFieldNoInherit(builtinClass, "typeIndexID");
      JCasClassInfo jcasClassInfo = createJCasClassInfo(builtinClass, ti, jcasType); 
      jcasClassesInfoForBuiltins[ti.getCode()] = jcasClassInfo; 
    }
    
    for (TypeImpl subType : ti.getDirectSubtypes()) {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(subType);
      loadBuiltins(subType, cl);
    }
  }
    
  /**
   * Load JCas types for some combination of class loader and type system
   * These classes may have already been loaded for this type system
   * These classes may have already been loaded (perhaps for another type system)
   * @param ts 
   * @param isDoUserJCasLoading
   * @param cl
   */
  static void loadAtTypeSystemCommitTime(TypeSystemImpl ts, boolean isDoUserJCasLoading, ClassLoader cl) { 

    boolean alreadyLoaded = false;
    Map<String, JCasClassInfo> t2jcci;

    synchronized (cl2type2JCas) {
      t2jcci = cl2type2JCas.get(cl);
    
      if (null == t2jcci) {    
        t2jcci = new HashMap<>();
        cl2type2JCas.put(cl, t2jcci);
      } else {
        alreadyLoaded = true;
      }
    }
    
    /**
     * copy in built-ins
     *   update t2jcci (if not already loaded) with load info for type
     *   update type system's map from unique JCasID to the type in this type system
     */
    lookup = defaultLookup;
    for (int typecode = 1; typecode < jcasClassesInfoForBuiltins.length; typecode++) {
  
      JCasClassInfo jcci = jcasClassesInfoForBuiltins[typecode];
      if (jcci != null) {
        Class<?> jcasClass = jcci.jcasClass;  

        if (!alreadyLoaded) {
          t2jcci.put(jcasClass.getCanonicalName(), jcci);
        }
        setTypeFromJCasIDforBuiltIns(jcci, ts, typecode);
      }
    }  
    
    /**
     * Add all user-defined JCas Types, in subsumption order
     *   We add these now, in case JCas is turned on later - unless specifically
     *   specified to run without user-defined JCas loading
     */
    
    if (isDoUserJCasLoading) {
      /**
       * Two passes are needed loading is needed.  
       *   - The first one loads the JCas Cover Classes initializes everything
       *      -- some of the classes might already be loaded (including the builtins which are loaded once per class loader)
       *   - The second pass performs the conformance checks between the loaded JCas cover classes, and the current type system.
       *     This depends on having the TypeImpl's javaClass field be accurate (reflect any loaded JCas types)
       */
      
      try {
        Class<?> clazz = Class.forName(UIMAClassLoader.MHLC, true, cl);
        Method m = clazz.getMethod("getMethodHandlesLookup");
        lookup = (Lookup) m.invoke(null);
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
      }

      maybeLoadJCasAndSubtypes(ts, ts.topType, t2jcci.get(TOP.class.getCanonicalName()), cl, t2jcci);
      checkConformance(ts, ts.topType, t2jcci);
    }
        
    reportErrors();
  }

  private static void setTypeFromJCasIDforBuiltIns(JCasClassInfo jcci, TypeSystemImpl tsi, int typeCode) {
    int v = jcci.jcasType;
    // v is negative if not found, which is the case for types like FloatList (these can't be instantiated)
    if (v >= 0) {
      tsi.setJCasRegisteredType(v, tsi.getTypeForCode(typeCode));
    }
  }

  /**
   * Called for all the types, including the built-ins, but the built-ins have already been set up by the caller.
   * Saves the results in two places
   *   type system independent spot: JCasClassInfo instance indexed by JCasClassName
   *   type system spot: the JCasIndexID -> type table in the type system
   * @param ts the type system
   * @param ti the type to process
   * @param copyDownDefault_jcasClassInfo
   * @param cl the loader used to load, and to save the results under the key of the class loader the results
   * @param type2JCas map holding the results of loading JCas classes
   */
  private static void maybeLoadJCasAndSubtypes(
      TypeSystemImpl ts, 
      TypeImpl ti, 
      JCasClassInfo copyDownDefault_jcasClassInfo,
      ClassLoader cl,
      Map<String, JCasClassInfo> type2JCas) {
        
    String t2jcciKey = Misc.typeName2ClassName(ti.getName());
    JCasClassInfo jcci = type2JCas.get(t2jcciKey);
    boolean isCopyDown = true;

    if (jcci == null) {

      // not yet recorded as loaded under this class loader.
    
      Class<?> clazz = maybeLoadJCas(ti, cl);
      if (null != clazz && TOP.class.isAssignableFrom(clazz)) {
        
        int jcasType = -1;
        if (!Modifier.isAbstract(clazz.getModifiers())) { // skip next for abstract classes
          jcasType = Misc.getStaticIntFieldNoInherit(clazz, "typeIndexID");
          // if jcasType is negative, this means there's no value for this field
          assert(jcasType >= 0);
        }         
        jcci = createJCasClassInfo(clazz, ti, jcasType);
        isCopyDown = false;
        if (clazz != TOP.class) {  // TOP has no super class
          validateSuperClass(jcci, ti);
        }
      } else {
        jcci = copyDownDefault_jcasClassInfo;
      }
      
      type2JCas.put(t2jcciKey, jcci);

    } else {
      // this UIMA type was set up (maybe loaded, maybe defaulted to a copy-down) previously
      isCopyDown = jcci.isCopydown(t2jcciKey);

      if (isCopyDown) {
        // the "stored" version might have the wrong super class for this type system
        type2JCas.put(t2jcciKey, jcci = copyDownDefault_jcasClassInfo);
        
      } else if (!ti.isTopType()) {
        // strong test for non-copy-down case: supertype must match, with 2 exceptions
        validateSuperClass(jcci, ti);
      }
    }
       
    // this is done even after the class is first loaded, in case the type system changed.
    // don't set anything if copy down - otherwise was setting the copyed-down typeId ref to the 
    //   new ti
//    System.out.println("debug set jcas regisered type " + jcci.jcasType + ",  type = " + ti.getName());

    if (jcci.jcasType >= 0 && ! isCopyDown) {
      ts.setJCasRegisteredType(jcci.jcasType, ti); 
    }
    
    if (!ti.isPrimitive()) {  // bypass this for primitives because the jcasClassInfo is the "inherited one" of TOP
      /**
       * Note: this value sets into the shared TypeImpl (maybe shared among many JCas impls) the "latest" jcasClass
       * It is "read" by the conformance testing, while still under the type system lock.
       * Other uses of this may get an arbitrary (the latest) version of the class
       * Currently the only other use is in backwards compatibility with low level type system "switching" an existing type.
       */
      ti.setJavaClass(jcci.jcasClass);
    }
    
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(subtype);
      maybeLoadJCasAndSubtypes(ts, subtype, jcci, cl, type2JCas);
    }
  }

  private static String superTypeJCasName(TypeImpl ti) {
    return Misc.typeName2ClassName(ti.getSuperType().getName());
  }
  /**
   * verify that the supertype class chain matches the type
   * @param clazz -
   * @param ti -
   */
  private static void validateSuperClass(JCasClassInfo jcci, TypeImpl ti) {
    final Class<?> clazz = jcci.jcasClass; 
    if (! clazz.getSuperclass().getCanonicalName().equals(superTypeJCasName(ti))) {
      /** Special case exceptions */
      TypeImpl superti = ti.getSuperType();
      TypeSystemImpl tsi = ti.getTypeSystem();
      if (superti == tsi.arrayBaseType ||
          superti == tsi.listBaseType) return;
      /** The JCas class: "{0}" has supertype: "{1}" which doesn''t  match the UIMA type "{2}"''s supertype "{3}". */
      throw new CASRuntimeException(CASRuntimeException.JCAS_MISMATCH_SUPERTYPE,
        clazz.getCanonicalName(), 
        clazz.getSuperclass().getCanonicalName(),
        ti.getName(),
        ti.getSuperType().getName());
    }

  }
  
  /**
   * Called to load (if possible) a corresponding JCas class for a UIMA type.
   * Called at Class Init time for built-in types
   * Called at TypeSystemCommit for non-built-in types
   *   Runs the static initializers in the loaded JCas classes - doing resolve
   *   
   * Synchronization: done outside this class
   *   
   * @param typeName -
   * @param cl the class loader to use
   * @return the loaded / resolved class
   */
  private static Class<?> maybeLoadJCas(TypeImpl ti, ClassLoader cl) {
    Class<?> clazz = null;
    String className = Misc.typeName2ClassName(ti.getName());
    
    try {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(ti);
      clazz = Class.forName(className, true, cl);
    } catch (ClassNotFoundException e) {
      // This is normal, if there is no JCas for this class
    } finally {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(null);
    }
    return clazz;
  }
        
  /**
   * Return a Functional Interface for a generator for creating instances of a type.
   *   Function takes a casImpl arg, and returning an instance of the JCas type.
   * @param jcasClass the class of the JCas type to construct
   * @param typeImpl the UIMA type
   * @return a Functional Interface whose createFS method takes a casImpl 
   *         and when subsequently invoked, returns a new instance of the class
   */
  private static FsGenerator createGenerator(Class<?> jcasClass) {
    try {
      
      MethodHandle mh = lookup.findConstructor(jcasClass, findConstructorJCasCoverType);
      MethodType mtThisGenerator = methodType(jcasClass, TypeImpl.class, CASImpl.class);
 
      CallSite callSite = LambdaMetafactory.metafactory(
          lookup, // lookup context for the constructor 
          "createFS", // name of the method in the Function Interface 
          callsiteFsGenerator, // signature of callsite, return type is functional interface, args are captured args if any
          fsGeneratorType,  // samMethodType signature and return type of method impl by function object 
          mh,  // method handle to constructor 
          mtThisGenerator);
      return (FsGenerator) callSite.getTarget().invokeExact();
    } catch (Throwable e) {
      if (e instanceof NoSuchMethodException) {
        String classname = jcasClass.getName();
        add2errors(errorSet, new CASRuntimeException(e, CASRuntimeException.JCAS_CAS_NOT_V3, 
            classname,
            jcasClass.getClassLoader().getResource(classname.replace('.', '/') + ".class").toString()
            ));
        return null;
      }
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
  }
  
//  /**
//   * Return a Functional Interface for a getter for getting the value of a feature, 
//   * called by APIs using the non-JCas style of access via features, 
//   * but accessing the values via the JCas cover class getter methods.
//   * 
//   * The caller of these methods is the FeatureStructureImplC methods.  
//   * 
//   * There are these return values:
//   *   boolean, byte, short, int, long, float, double, String, FeatureStructure
//   *   
//   */
//  // static for setting up builtin values
//  private static Object createGetterOrSetter(Class<?> jcasClass, FeatureImpl fi, boolean isGetter, boolean ncnj) {
//    
//    TypeImpl range = fi.getRangeImpl();
//    String name = ncnj ? ("_" + fi.getGetterSetterName(isGetter) + "NcNj")
//                       :        fi.getGetterSetterName(isGetter); 
//    
//    try {
//      /* get an early-bound getter    
//      /* Instead of findSpecial, we use findVirtual, in case the method is overridden by a subtype loaded later */
//      MethodHandle mh = lookup.findVirtual(
//          jcasClass,  // class having the method code for the getter 
//          name,       // the name of the method for the getter or setter 
//          isGetter ? methodType(range.javaClass)
//                   : methodType(void.class, range.javaClass) // return value, e.g. int.class, xyz.class, FeatureStructureImplC.class
//        );
//      
//      // getter methodtype is return_type, FeatureStructure.class
//      //   return_type is int, byte, etc. primitive (except string/substring), or
//      //   object (to correspond with erasure)
//      // setter methodtype is void.class, FeatureStructure.class, javaclass
//      MethodType mhMt = isGetter ? methodType(range.getJavaPrimitiveClassOrObject(), FeatureStructureImplC.class)
//                                 : methodType(void.class, FeatureStructureImplC.class, range.getJavaPrimitiveClassOrObject());
//      MethodType iMt =  isGetter ? methodType(range.javaClass, jcasClass)
//                                 : methodType(void.class, jcasClass, range.javaClass);
//      
////      System.out.format("mh method type for %s method %s is %s%n", 
////          jcasClass.getSimpleName(), 
////          fi.getGetterSetterName(isGetter),
////          mhMt);
//          
//      CallSite callSite = LambdaMetafactory.metafactory(
//          lookup,     // lookup context for the getter
//          isGetter ? "get" : "set", // name of the method in the Function Interface                 
//          methodType(isGetter ? range.getter_funct_intfc_class : range.setter_funct_intfc_class),  // callsite signature = just the functional interface return value
//          mhMt,                      // samMethodType signature and return type of method impl by function object 
//          mh,  // method handle to constructor 
//          iMt);
//    
//      if (range.getJavaClass() == boolean.class) {
//        return isGetter ? (JCas_getter_boolean) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_boolean) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == byte.class) {
//        return isGetter ? (JCas_getter_byte) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_byte) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == short.class) {
//        return isGetter ? (JCas_getter_short) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_short) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == int.class) {
//        return isGetter ? (JCas_getter_int) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_int) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == long.class) {
//        return isGetter ? (JCas_getter_long) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_long) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == float.class) {
//        return isGetter ? (JCas_getter_float) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_float) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == double.class) {
//        return isGetter ? (JCas_getter_double) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_double) callSite.getTarget().invokeExact();
//      } else {
//        return isGetter ? (JCas_getter_generic<?>) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_generic<?>) callSite.getTarget().invokeExact();
//      }
//    } catch (NoSuchMethodException e) {
//      if ((jcasClass == Sofa.class && !isGetter) ||
//          (jcasClass == AnnotationBase.class && !isGetter)) {
//        return null;
//      }  
//      // report missing setter or getter
//      /* Unable to find required {0} method for JCAS type {1} with {2} type of {3}. */
//      CASException casEx = new CASException(CASException.JCAS_GETTER_SETTER_MISSING, 
//          name,
//          jcasClass.getName(),
//          isGetter ? "return" : "argument",
//          range.javaClass.getName()     
//          );
//      ArrayList<Exception> es = errorSet.get();
//      if (es == null) {
//        es = new ArrayList<Exception>();
//        errorSet.set(es);
//      }
//      es.add(casEx);
//      return null;
//    } catch (Throwable e) {
//      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
//    }
//  }
   
//  GetterSetter getGetterSetter(int typecode, String featShortName) {
//    return jcasClassesInfo[typecode].gettersAndSetters.get(featShortName);
//  }
   
  // static for setting up static builtin values
  /**
   * Called after succeeding at loading, once per load for an exact matching JCas Class 
   *   - class was already checked to insure is of proper type for JCas
   *   - skips creating-generator-for-Sofa - since "new Sofa(...)" is not a valid way to create a sofa   
   * 
   * @param jcasClass the JCas class that corresponds to the type
   * @param ti the type
   * @return the info for this JCas that is shared across all type systems under this class loader
   */
  private static JCasClassInfo createJCasClassInfo(Class<?> jcasClass, TypeImpl ti, int jcasType) {
    boolean noGenerator = ti.getCode() == TypeSystemConstants.sofaTypeCode ||
                          Modifier.isAbstract(jcasClass.getModifiers()) ||
                          ti.isArray(); 
    FsGenerator generator = noGenerator ? null : createGenerator(jcasClass);
    JCasClassInfo jcasClassInfo = new JCasClassInfo(jcasClass, generator, jcasType);
//    System.out.println("debug creating jcci, classname = " + jcasClass.getName() + ", jcasTypeNumber: " + jcasType);
    return jcasClassInfo;
  }
  
//  static boolean isFieldInClass(Feature feat, Class<?> clazz) {
//    try {
//      return null != clazz.getDeclaredField("_FI_" + feat.getShortName());
//    } catch (NoSuchFieldException e) {
//      return false;
//    }    
//  }
  
  
  private static void checkConformance(TypeSystemImpl ts, TypeImpl ti, Map<String, JCasClassInfo> type2jcci) {
    if (ti.isPrimitive()) return;
    JCasClassInfo jcasClassInfo = type2jcci.get(ti.getName());
    if (null != jcasClassInfo) { // skip if the UIMA class has an abstract (non-creatable) JCas class)      
      checkConformance(jcasClassInfo.jcasClass, ts, ti, type2jcci);
    }
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      checkConformance(ts, subtype, type2jcci);
    }
  }
  
  /**
   * Checks that a JCas class definition conforms to the current type in the current type system.
   * Checks that the superclass chain contains some match to the super type chain.
   * Checks that the return value for the getters for features matches the feature's range.
   * Checks that static _FI_xxx values from the JCas class == the adjusted feature offsets in the type system
   * 
   * @param clazz - the JCas class to check
   * @param tsi -
   * @param ti -
   */
  private static void checkConformance(Class<?> clazz, TypeSystemImpl tsi, TypeImpl ti, Map<String, JCasClassInfo> type2jcci) {

    // skip the test if the jcasClassInfo is being inherited
    //   because that has already been checked
    if (!clazz.getName().equals(Misc.typeName2ClassName(ti.getName()))) {
      return;
    }
    
    // check supertype
         
    // one of the supertypes must match a superclass of the class
    boolean isOk = false;
    List<Class<?>> superClasses = new ArrayList<>();
   outer:
    for (TypeImpl superType : ti.getAllSuperTypes()) {
      JCasClassInfo jci = type2jcci.get(superType.getName());
      if (jci == null) continue;
      Class<?> superClass = clazz.getSuperclass();
      superClasses.add(superClass);
      while (superClass != FeatureStructureImplC.class && superClass != Object.class) {
        if (jci.jcasClass == superClass) {
          isOk = true;
          break outer;
        }
        superClass = superClass.getSuperclass();
        superClasses.add(superClass);
      }
    }
    
    if (!isOk && superClasses.size() > 0) {
      /** JCas Class's supertypes for "{0}", "{1}" and the corresponding UIMA Supertypes for "{2}", "{3}" don't have an intersection. */
      add2errors(errorSet, 
                 new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH_SUPERTYPE, 
                     clazz.getName(), Misc.ppList(superClasses), ti.getName(), Misc.ppList(Arrays.asList(ti.getAllSuperTypes()))),
                 true);  // throwable error
    }

    // the range of all the features must match the getters

    for (Method m : clazz.getDeclaredMethods()) {
      
      String mname = m.getName(); 
      if (mname.length() <= 3 || !mname.startsWith("get")) continue;
      String suffix = (mname.length() == 4) ? "" : mname.substring(4); 
      String fname = Character.toLowerCase(mname.charAt(3)) + suffix; 
      FeatureImpl fi = ti.getFeatureByBaseName(fname);
      if (fi == null) {
        fname = mname.charAt(3) + suffix;
        fi = ti.getFeatureByBaseName(fname);
        if (fi == null) continue;
      }
      
      // have the feature, check the range
      Class<?> returnClass = m.getReturnType(); // for primitive, is int.class, etc.
      TypeImpl range = fi.getRangeImpl();
      Class<?> rangeClass = range.getJavaClass();
      if (fi.getRangeImpl().isArray()) {
        Parameter[] p = m.getParameters();
        if (p.length == 1 && p[0].getType() == int.class) {
          rangeClass = range.getComponentType().getJavaClass();
        }
      }
      if (!rangeClass.isAssignableFrom(returnClass)) {   // can return subclass of TOP, OK if range is TOP
        if (rangeClass.getName().equals("org.apache.uima.jcas.cas.Sofa") &&       // exception: for backwards compat reasons, sofaRef returns SofaFS, not Sofa.
            returnClass.getName().equals("org.apache.uima.cas.SofaFS")) {
          // empty
        } else {
          
          /** CAS type system type "{0}" defines field "{1}" with range "{2}", but JCas getter method is returning "{3}" which is not a subtype of the declared range.*/
          add2errors(errorSet, 
                     new CASRuntimeException(CASRuntimeException.JCAS_TYPE_RANGE_MISMATCH, 
                         ti.getName(), fi.getShortName(), rangeClass, returnClass),
                     false);  // should throw, but some code breaks!
        }
      }
    }
    
    for (Field f : clazz.getDeclaredFields()) {
      String fname = f.getName();
      if (fname.length() <= 5 || !fname.startsWith("_FI_")) continue;
      String featName = fname.substring(4);
      FeatureImpl fi = ti.getFeatureByBaseName(featName);
      if (fi == null) {
        add2errors(errorSet, 
                   new CASRuntimeException(CASRuntimeException.JCAS_FIELD_MISSING_IN_TYPE_SYSTEM, clazz.getName(), featName), 
                   false);  // don't throw on this error, field is set to -1 and will throw if trying to use it   
       } else {
        int staticOffsetInClass = Misc.getStaticIntFieldNoInherit(clazz, fname);
        if (fi.getAdjustedOffset() != staticOffsetInClass) {
          /** In JCAS class "{0}", UIMA field "{1}" was set up when this class was previously loaded and initialized, to have
           * an adjusted offset of "{2}" but now the feature has a different adjusted offset of "{3}"; this may be due to 
           * something else other than type system commit actions loading and initializing the JCas class, or to
           * having a different non-compatible type system for this class, trying to use a common JCas cover class, which is not supported. */
          add2errors(errorSet, 
                     new CASRuntimeException(CASRuntimeException.JCAS_FIELD_ADJ_OFFSET_CHANGED,
                        clazz.getName(), 
                        fi.getName(), 
                        Integer.valueOf(staticOffsetInClass), 
                        Integer.valueOf(fi.getAdjustedOffset())),
                     staticOffsetInClass != -1);  // throw unless static offset is -1, in that case, a runtime error will occur if it is usedd
        }
      }
    }
  }
  
  private static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e) {
    add2errors(errors, e, true);
  }
  
  private static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e, boolean doThrow) {
    List<ErrorReport> es = errors.get();
    if (es == null) {
      es = new ArrayList<ErrorReport>();
      errors.set(es);
    }
    es.add(new ErrorReport(e, doThrow));
  }
  
  private static void reportErrors() {
    boolean throwWhenDone = false;
    List<ErrorReport> es = errorSet.get();
    if (es != null) {
      StringBuilder msg = new StringBuilder(100);
      msg.append('\n');
      for (ErrorReport f : es) {
        msg.append(f.e.getMessage());
        throwWhenDone = throwWhenDone || f.doThrow;
        msg.append('\n');
      }
      errorSet.set(null); // reset after reporting
      if (throwWhenDone) {
        throw new CASRuntimeException(CASException.JCAS_INIT_ERROR, msg);
      } else {
        Logger logger = UIMAFramework.getLogger();
        if (null == logger) {
          throw new CASRuntimeException(CASException.JCAS_INIT_ERROR, msg);
        } else {
          logger.log(Level.WARNING, msg.toString());
        }          
      }
    }
  }  
  
  /**
   * called infrequently to set up cache
   * Only called when a type system has not had generators for a particular class loader.
   * 
   * For PEAR generators: 
   *   Populates only for those classes the PEAR has overriding implementations
   *     - other entries are null; this serves as a boolean indicator that no pear override exists for that type
   *       and therefore no trampoline is needed
   * 
   * @param cl identifies which set of jcas cover classes
   * @param isPear true for pear case
   * @param tsi the type system being used
   * @return the generators for that set, as an array indexed by type code
   */
  static FsGenerator[] getGeneratorsForClassLoader(ClassLoader cl, boolean isPear, TypeSystemImpl tsi) {
    synchronized(cl2type2JCas) {
      // This is the first time this class loader is being used - load the classes for this type system, or
      // This is the first time this class loader is being used with this particular type system
      loadAtTypeSystemCommitTime(tsi, true, cl);      

      FsGenerator[] r = new FsGenerator[tsi.getTypeArraySize()];
                          
      Map<String, JCasClassInfo> t2jcci = cl2type2JCas.get(cl);
      // can't use values alone because many types have the same value (due to copy-down)
      for (Entry<String, JCasClassInfo> e : t2jcci.entrySet()) {
        TypeImpl ti = tsi.getType(Misc.javaClassName2UimaTypeName(e.getKey()));
        if (null == ti) {
          continue;  // JCas loaded some type in the past, but it's not in this type system
        }
        JCasClassInfo jcci = e.getValue();
        
        if (!isPear || jcci.isPearOverride(cl)) {
          r[ti.getCode()] = (FsGenerator) jcci.generator;
        }      
      }
      return r;
    }   
  }
  
  private static boolean isAllNull(FsGenerator[] r) {
    for (FsGenerator v : r) {
      if (v != null)
        return false;
    }
    return true;
  }

}
  
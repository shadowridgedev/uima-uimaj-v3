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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.CommonArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.JavaObjectArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
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
public class FeatureStructureImplC implements FeatureStructure, Cloneable, Comparable<FeatureStructure> {

  // note: these must be enabled to make the test cases work
  public static final String DISABLE_RUNTIME_FEATURE_VALIDATION = "uima.disable_runtime_feature_validation";
  public static final boolean IS_ENABLE_RUNTIME_FEATURE_VALIDATION  = !Misc.getNoValueSystemProperty(DISABLE_RUNTIME_FEATURE_VALIDATION);

  public static final String DISABLE_RUNTIME_FEATURE_VALUE_VALIDATION = "uima.disable_runtime_feature_value_validation";
  public static final boolean IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION  = !Misc.getNoValueSystemProperty(DISABLE_RUNTIME_FEATURE_VALUE_VALIDATION);

  public static final String DISABLE_RUNTIME_FEATURE_RANGE_VALIDATION = "uima.disable_runtime_feature_range_validation";
  public static final boolean IS_ENABLE_RUNTIME_FEATURE_RANGE_VALIDATION  = !Misc.getNoValueSystemProperty(DISABLE_RUNTIME_FEATURE_RANGE_VALIDATION);

  public static final int IN_SET_SORTED_INDEX = 1;
  // data storage
  // slots start with _ to prevent name collision with JCas style getters and setters.
  
  protected final int[] _intData;  
  protected final Object[] _refData;
  protected final int _id;  // a separate slot for access without loading _intData object
  protected int flags = 0;  // a set of flags
                            // bit 0 (least significant): fs is in one or more non-bag indexes
                            // bits 1-31 reserved
                           
  
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
  
  public final TypeImpl _typeImpl;
  
  // Called only to generate a dummy value for the REMOVED flag in bag indexes

  public FeatureStructureImplC() {
    _casView = null;
    _typeImpl = null;
    _intData = null;
    _refData = null;
    _id = 0;    
  }
  
  /** 
   * For use in creating search keys
   * @param id
   */
  protected FeatureStructureImplC(int id) {
    _casView = null;
    _typeImpl = null;
    _intData = null;
    _refData = null;
    _id = id;    
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
    
    _id = casView.setId2fs((TOP)this);    
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
    
    _id = _casView.setId2fs((TOP)this); 
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
  public final int id() {return _id; };
  
  /**
   * Returns the UIMA TypeImpl value
   */
  @Override
  public Type getType() {
    return _typeImpl;
  }
  
  public TypeImpl getTypeImpl() {
    return _typeImpl;
  }
  
  /**
   * starts with _ 
   * @return the UIMA TypeImpl for this Feature Structure
   */
@Override
  public int _getTypeCode() {
    return _typeImpl.getCode();
  }
  
  public CASImpl _getView() {
    return _casView;
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

  /**************************************
   *           S E T T E R S 
   * 4 levels:  
   *   - check feature for validity
   *     -- this is skipped with feature comes from fs type info (internal calls)
   *   - check for setting something which could corrupt indexes
   *     -- this is skipped when the caller knows 
   *        --- the FS is not in the index, perhpas because they just created it
   *     -- skipped when the range is not a valid index key   
   *   - check for needing to log (journal) setting
   *     -- this is skipped when the caller knows 
   *       --- no journalling is enabled or
   *       --- the FS is a new (above-the-line) FS
   *   - check the value is suitable
   *     -- this can be skipped if Java is doing the checking (via the type of the argument)
   *     -- done for string subtypes and Feature References
   *       --- skipped if the caller knows the value is OK (e.g., it is copying an existing FS)
   *       
   * all 4 checks are normally done by the standard API call in the FeatureStructure interface 
   *    setXyzValue(Feature, value)
   *    
   * Other methods have suffixes and prefixes to the setter name
   *   - prefix is "_" to avoid conflicting with existing other names
   *   - suffixes are: 
   *     -- Nfc:    skip feature validity checking
   *     -- NcNj:   implies Nfc, skips corrupt check and journaling and feature validation
   *          The next two are only for setters where value checking might be needed (i.e., Java checking isn't sufficient)
   *     -- Nv:     implies Nfc, skips value range checking and feature validation
   *     -- NcNjNv: implies Nfc, skips all checks
   *     
   *          For JCas setters: convert offset to feature
   **************************************/
  
  @Override
  public void setBooleanValue(Feature feat, boolean v) {
    _setIntValueCJ((FeatureImpl) feat, v ? 1 : 0);
  }
  
  public void _setBooleanValueNfc(FeatureImpl feat, boolean v) { _setIntValueNfcCJ(feat, v ? 1 : 0); }
 
  public final void _setBooleanValueNcNj(FeatureImpl fi, boolean v) { _setIntValueCommon(fi, v? 1 : 0); }
   
  public final void _setBooleanValueNcNj(int adjOffset, boolean v) { _setIntValueCommon(adjOffset, v? 1 : 0); }
 
  @Override
  public void setByteValue(Feature feat, byte v) {
    _setIntValueCJ((FeatureImpl) feat, v);
  }
  
  public void _setByteValueNfc(FeatureImpl fi, byte v) {
    _setIntValueNfcCJ(fi, v);
  }
  
  public void _setByteValueNcNj(FeatureImpl fi, byte v) {
    _setIntValueCommon(fi, v);
  }

  public void _setByteValueNcNj(int adjOffset, byte v) {
    _setIntValueCommon(adjOffset, v);
  }

  @Override
  public void setShortValue(Feature feat, short v) {
    _setIntValueCJ((FeatureImpl) feat, v);
  }
  
  public void _setShortValueNfc(FeatureImpl fi, short v) {
    _setIntValueNfcCJ(fi, v);
  }
  
  public void _setShortValueNcNj(FeatureImpl fi, short v) {
    _setIntValueCommon(fi, v);
  }

  public void _setShortValueNcNj(int adjOffset, short v) {
    _setIntValueCommon(adjOffset, v);
  }

  @Override
  public void setIntValue(Feature feat, int v) {
    _setIntValueCJ((FeatureImpl) feat, v);
  }
  
  public void _setIntValueNfc(FeatureImpl fi, int v) {
    _setIntValueNfcCJ(fi, v);
  }
  
  public void _setIntValueNcNj(FeatureImpl fi, int v) {
    _setIntValueCommon(fi, v);
  }

  public void _setIntValueNcNj(int adjOffset, int v) {
    _setIntValueCommon(adjOffset, v);
  }

  @Override
  public void setLongValue(Feature feat, long v) {
    _setLongValueCJ((FeatureImpl) feat, v);
  }

  public void _setLongValueNfc(FeatureImpl fi, long v) {
    _setLongValueNfcCJ(fi, v);
  }
  
  public void _setLongValueNcNj(FeatureImpl fi, long v) { _setLongValueNcNj(fi.getAdjustedOffset(), v); }

  public void _setLongValueNcNj(int adjOffset, long v) {
    _intData[adjOffset] = (int)(v & 0xffffffff);
    _intData[adjOffset + 1] = (int)(v >> 32);
  }

  @Override
  public void setFloatValue(Feature feat, float v) { setIntValue(feat, CASImpl.float2int(v)); }
  
  protected void _setFloatValueNfc(FeatureImpl feat, float v) { _setIntValueNfc(feat, CASImpl.float2int(v)); }

  public void _setFloatValueNcNj(FeatureImpl fi, float v) {
    _intData[fi.getAdjustedOffset()] = CASImpl.float2int(v);
  }

  public void _setFloatValueNcNj(int adjOffset, float v) {
    _intData[adjOffset] = CASImpl.float2int(v);
  }

  @Override
  public void setDoubleValue(Feature feat, double v) { setLongValue(feat, CASImpl.double2long(v)); }

  protected void _setDoubleValueNfc(FeatureImpl feat, double v) { _setLongValueNfcCJ(feat, CASImpl.double2long(v)); }

  public void _setDoubleValueNcNj(FeatureImpl fi, double v) { _setLongValueNcNj(fi, CASImpl.double2long(v)); }

  public void _setDoubleValueNcNj(int adjOffset, double v) { _setLongValueNcNj(adjOffset, CASImpl.double2long(v)); }

  @Override
  public void setStringValue(Feature feat, String v) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    subStringRangeCheck(feat, v);
    _setRefValueCJ((FeatureImpl) feat, v);
  }
  
  public void _setStringValueNfc(FeatureImpl fi, String v) {
    subStringRangeCheck(fi, v); 
    _setRefValueCJ(fi, v);
  }

  public void _setStringValueNcNj(FeatureImpl fi, String v) {
    subStringRangeCheck(fi, v); 
    _setRefValueCommon(fi, v);
  }
  
  /**
   * Skips substring range checking, but maybe does journalling
   * @param adjOffset offset
   * @param v to set
   */
  public void _setStringValueNcWj(int adjOffset, String v) {
    _setRefValueCommonWj(_getFeatFromAdjOffset(adjOffset, false), v);
  }

  @Override
  public void setFeatureValue(Feature feat, FeatureStructure v) {
    FeatureImpl fi = (FeatureImpl) feat;
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    if (IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION) featureValueValidation(feat, v);

    // no need to check for index corruption because fs refs can't be index keys
    _refData[fi.getAdjustedOffset()] = v;
    _casView.maybeLogUpdate(this, fi);
  }
  
  public void _setFeatureValueNcNj(FeatureImpl fi, Object v) { 
    _setRefValueCommon(fi, v);
  }

  public void _setFeatureValueNcWj(FeatureImpl fi, Object v) { 
    _setRefValueCommonWj(fi, v);
  }

  @Override
  public void setJavaObjectValue(Feature feat, Object v) { 
    FeatureImpl fi = (FeatureImpl) feat;
    
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    if (IS_ENABLE_RUNTIME_FEATURE_VALUE_VALIDATION) featureValueValidation(feat, v);
    _setRefValueCJ(fi, v);
  }
  
  public void _setJavaObjectValueNcNj(FeatureImpl fi, Object v) { 
    _setRefValueCommon(fi, v);
  }

  @Override
  public void setFeatureValueFromString(Feature feat, String s) throws CASRuntimeException {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    _casView.setFeatureValueFromString(this, (FeatureImpl) feat, s);
  }

  /**
   * All 3 checks
   * @param fi - the feature
   * @param v - the value
   */
  protected void _setIntValueCJ(FeatureImpl fi, int v) {
    if (!fi.isInInt) {
      /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}".*/
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(), "boolean, byte, short, int, or float", fi.getRange().getName());
    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(fi);
    _casView.setWithCheckAndJournal((TOP)this, fi.getCode(), () -> _intData[fi.getAdjustedOffset()] = v); 
  }
  
  /**
   * All 3 checks for long
   * @param fi - the feature
   * @param v - the value
   */
  protected void _setLongValueCJ(FeatureImpl fi, long v) {
    if (!fi.isInInt) {
      /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}".*/
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(), "long or double", fi.getRange().getName());
    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(fi);
    _casView.setLongValue(this, fi, v); 
  }

  
  /**
   * 2 checks, no feature check
   * @param fi - the feature
   * @param v - the value
   */
  protected void _setIntValueNfcCJ(FeatureImpl fi, int v) {
    _casView.setWithCheckAndJournal((TOP)this, fi.getCode(), () -> _intData[fi.getAdjustedOffset()] = v); 
  }
  
  /**
   * 2 checks, no feature check
   * @param fi - the feature
   * @param v - the value
   */
  protected void _setLongValueNfcCJ(FeatureImpl fi, long v) {
    _casView.setLongValue(this, fi, v); 
  }

  protected void _setRefValueCJ(FeatureImpl fi, Object v) {
    if (fi.isInInt) {
      /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}".*/
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(), "int", fi.getRange().getName());
    }
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(fi);
    _casView.setWithCheckAndJournal((TOP)this, fi.getCode(), () -> _refData[fi.getAdjustedOffset()] = v); 
  }
  
  /**
   * 2 checks, no feature check
   * @param fi - the feature
   * @param v - the value
   */
  protected void _setRefValueNfcCJ(FeatureImpl fi, Object v) {
    _casView.setWithCheckAndJournal((TOP)this, fi.getCode(), () -> _refData[fi.getAdjustedOffset()] = v); 
  }

  /********************************************************************************************************   
   *       G E T T E R S
   * 
   *  (The array getters are part of the Classes for the built-in arrays, here are only the non-array ones)
   *  
   *  getXyzValue(Feature feat) - this is the standard from V2 plain API
   *                            - it does validity checking (normally) that the feature belongs to the type
   *  getXyzValueNc(FeatureImpl feat) - skips the validity checking that the feature belongs to the type.                          
   *********************************************************************************************************/

  @Override
  public boolean getBooleanValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getBooleanValueNc((FeatureImpl) feat);
  }

  public boolean _getBooleanValueNc(FeatureImpl fi) { return _getIntValueCommon(fi) == 1; }
  
  // for JCas use
  public boolean _getBooleanValueNc(int adjOffset) { return _getIntValueCommon(adjOffset) == 1; }

  @Override
  public byte getByteValue(Feature feat) { return (byte) getIntValue(feat); }

  public byte _getByteValueNc(FeatureImpl feat) { return (byte) _getIntValueNc(feat); }
  
  public byte _getByteValueNc(int adjOffset) { return  (byte) _getIntValueNc(adjOffset); }

  @Override
  public short getShortValue(Feature feat) { return (short) getIntValue(feat); }

  public short _getShortValueNc(FeatureImpl feat) { return (short) _getIntValueNc(feat); }

  public short _getShortValueNc(int adjOffset) { return (short) _getIntValueNc(adjOffset); }

  @Override
  public int getIntValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getIntValueCommon((FeatureImpl)feat);
  }
    
  public int _getIntValueNc(FeatureImpl feat) { return _getIntValueCommon(feat); }
  
  public int _getIntValueNc(int adjOffset) { return _getIntValueCommon(adjOffset); }


  @Override
  public long getLongValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getLongValueNc((FeatureImpl) feat);
  }
  
  public long _getLongValueNc(FeatureImpl feat) { return _getLongValueNc(feat.getAdjustedOffset());
    
  }
  public long _getLongValueNc(int adjOffset) { 
    /**
     * When converting the lower 32 bits to a long, sign extension is done, so have to 
     * 0 out those bits before or-ing in the high order 32 bits.
     */
    return (((long)_intData[adjOffset]) & 0x00000000ffffffffL) | (((long)_intData[adjOffset + 1]) << 32); 
  }
  
  @Override
  public float getFloatValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getFloatValueNc(((FeatureImpl) feat).getAdjustedOffset());
  }

  public float _getFloatValueNc(FeatureImpl fi) { return _getFloatValueNc(fi.getAdjustedOffset()); }
  
  public float _getFloatValueNc(int adjOffset) { return CASImpl.int2float(_getIntValueCommon(adjOffset)); }

  @Override
  public double getDoubleValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getDoubleValueNc((FeatureImpl) feat); 
  }
  
  public double _getDoubleValueNc(FeatureImpl fi) { return _getDoubleValueNc(fi.getAdjustedOffset()); }
  
  public double _getDoubleValueNc(int adjOffset) { return CASImpl.long2double(_getLongValueNc(adjOffset)); }
  
  @Override
  public String getStringValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getStringValueNc((FeatureImpl) feat);
  }

  public String _getStringValueNc(FeatureImpl feat) { return _getStringValueNc(feat.getAdjustedOffset()); }

  public String _getStringValueNc(int adjOffset) { return (String) _refData[adjOffset]; }

  @Override
  public TOP getFeatureValue(Feature feat) {
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getFeatureValueNc((FeatureImpl) feat);
  }
  
  public TOP _getFeatureValueNc(FeatureImpl feat) { return (TOP) _getFeatureValueNc(feat.getAdjustedOffset()); }

  public TOP _getFeatureValueNc(int adjOffset) { return (TOP) _refData[adjOffset]; }
 
  @Override
  public Object getJavaObjectValue(Feature feat) { 
    if (IS_ENABLE_RUNTIME_FEATURE_VALIDATION) featureValidation(feat);
    return _getJavaObjectValueNc((FeatureImpl) feat);
  }

  public Object _getJavaObjectValueNc(FeatureImpl fi) { return _getRefValueCommon(fi); }

  public Object _getJavaObjectValueNc(int adjOffset) { return _getRefValueCommon(adjOffset); }

  /**
   * @return the CAS view where this FS was created
   */
  @Override
  public CAS getCAS() {
    return this._casView;
  }

  public CASImpl getCASImpl() { // was package private 9-03
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
      CommonArray copy = (CommonArray) _casView.createArray(_typeImpl, original.size());
      copy.copyValuesFrom(original);      
      return (FeatureStructureImplC) copy;
    }
    
    TOP fs = _casView.createFS(_typeImpl);
    TOP srcFs = (TOP) this;
    
    /* copy all the feature values except the sofa ref which is already set as part of creation */
    for (FeatureImpl feat : _typeImpl.getFeatureImpls()) {
      CASImpl.copyFeature(srcFs, feat, fs);
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
    if (ti != null) { // null for REMOVED marker
      if (ti.isArray() && (fs instanceof FSArray)) {
        for (TOP item : ((FSArray)fs)._getTheArray()) {
          getPrintRefs(printRefs, item);
        }
      } else {
        ti.getFeaturesAsStream()
          .filter(fi -> fi.getRangeImpl().isRefType)     // is ref type
          .map(fi -> this.getFeatureValue(fi)) // get the feature value
          .filter(refFs -> refFs != null)            // skip null ones
          .forEachOrdered(refFs -> getPrintRefs(printRefs, refFs));
      }
    }
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
    try {
    indent += incr;
    if (indent > 20) {
      buf.append(" ... past indent 20 ... ");
      return;
    }
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
    if (_typeImpl == null) {
      buf.append((_id == 0) ? " Special REMOVED marker " : " Special Search Key, id = " + _id);
    } else {
      if (useShortNames) {
        buf.append(getType().getShortName());
      } else {
        buf.append(getType().getName());
      }
      buf.append(':').append(_id);
      if (s != null) {
        buf.append(" \"" + s + "\"");
      }
    }
    buf.append('\n');

//    final int typeClass = this._casView.ll_getTypeClass(this.getType());
    
    if (_typeImpl == null) {  // happens for special version which is REMOVED marker
      return;
    }
    switch (_getTypeCode()) {
    case TypeSystemImpl.stringArrayTypeCode: {
      StringArray a = (StringArray) this;
      printArrayElements(a.size(), i -> a.get(i), indent, buf);
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
    
    for (FeatureImpl fi : _typeImpl.getFeatureImpls()) {
      StringUtils.printSpaces(indent, buf);
      buf.append(fi.getShortName() + ": ");
      TypeImpl range = (TypeImpl) fi.getRange();
      
      if (range.isStringOrStringSubtype()) {
        String stringVal = getStringValue(fi);
        stringVal = (null == stringVal) ? "<null>" : "\"" + Misc.elideString(stringVal, 15) + "\"";
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
          if (val != null && !fi.getName().equals(CAS.TYPE_NAME_SOFA)) {
            ((FeatureStructureImplC) val).prettyPrint(indent, incr, buf, useShortNames, null, printRefs);
          } else {
            buf.append((val == null) ? "<null>\n" : ((SofaFS) val).getSofaID() + '\n'); 
          }
        }
    
      } else {  
        // is primitive
        buf.append(this.getFeatureValueAsString(fi) + "\n");
      }
    }
    } catch (Exception e) {
      buf.append("**Caught exception: ").append(e);
//      StringWriter sw = new StringWriter();
//      e.printStackTrace(new PrintWriter(sw, true));
//      buf.append(sw.toString());
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
        if (null == element) {
          buf.append("null");
        } else {
          buf.append("\"" + Misc.elideString(element, 15) + "\"");
        }
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
  
  /**
   * Internal Use only
   * @param slotKind -
   * @param fi -
   * @param v -
   */
  public void _setIntLikeValue(SlotKind slotKind, FeatureImpl fi, int v) {
    switch(slotKind) {
    case Slot_Boolean: setBooleanValue(fi, v == 1); break;
    case Slot_Byte: setByteValue(fi, (byte) v); break;
    case Slot_Short: setShortValue(fi, (short) v); break;
    case Slot_Int: setIntValue(fi, v); break;
    case Slot_Float: setFloatValue(fi, CASImpl.int2float(v)); break;
    default: Misc.internalError();
    }
  }
  
  /**
   * Internal Use only - no feature check, no journaling
   * @param slotKind -
   * @param fi -
   * @param v -
   */
  public void _setIntLikeValueNcNj(SlotKind slotKind, FeatureImpl fi, int v) {
    switch(slotKind) {
    case Slot_Boolean: _setBooleanValueNcNj(fi, v == 1); break;
    case Slot_Byte: _setByteValueNcNj(fi, (byte) v); break;
    case Slot_Short: _setShortValueNcNj(fi, (short) v); break;
    case Slot_Int: _setIntValueNcNj(fi, v); break;
    case Slot_Float: _setFloatValueNcNj(fi, CASImpl.int2float(v)); break;
    default: Misc.internalError();
    }
  }

  
  /**
   * for compressed form 4 - for getting the prev value of int-like slots
   * Uses unchecked forms for feature access
   * @param slotKind
   * @param fi
   * @param v
   */
  public int _getIntLikeValue(SlotKind slotKind, FeatureImpl f) {
    if (null == f) {
      switch(slotKind) {
      
      case Slot_Boolean: {
        BooleanArray a = (BooleanArray)this;
        return (a.size() == 0) ? 0 : a.get(0) ? 1 : 0;
      }
      
      case Slot_Byte: {
        ByteArray a = (ByteArray)this;
        return (a.size() == 0) ? 0 : a.get(0);
      }
      
      case Slot_Short: {
        ShortArray a = (ShortArray)this;
        return (a.size() == 0) ? 0 : a.get(0);
      }
      
      case Slot_Int: {
        IntegerArray a = (IntegerArray)this;
        return (a.size() == 0) ? 0 : a.get(0);
      }

      case Slot_Float: {
        FloatArray a = (FloatArray)this;
        return (a.size() == 0) ? 0 : CASImpl.float2int(a.get(0));
      }
      default: Misc.internalError(); return 0;
      }
    }
    
    switch(slotKind) {
    case Slot_Boolean: return _getBooleanValueNc(f) ? 1 : 0;
    case Slot_Byte: return _getByteValueNc(f);
    case Slot_Short: return _getShortValueNc(f);
    case Slot_Int: return _getIntValueNc(f);
    case Slot_Float: return CASImpl.float2int(_getFloatValueNc(f));
    default: Misc.internalError(); return 0;
    }
  }

  @Override
  public String getFeatureValueAsString(Feature feat) {
    FeatureImpl fi = (FeatureImpl) feat;
    TypeImpl range = fi.getRangeImpl();
    if (fi.isInInt) {
      switch (range.getCode()) {
      case TypeSystemImpl.floatTypeCode :
        return Float.toString(getFloatValue(feat));
      case TypeSystemImpl.booleanTypeCode :
        return Boolean.toString(getBooleanValue(feat));
      case TypeSystemImpl.longTypeCode :
        return Long.toString(getLongValue(feat));
      case TypeSystemImpl.doubleTypeCode :
        return Double.toString(getDoubleValue(feat));
      default: 
        return Integer.toString(getIntValue(feat));
      }
    }
    
    if (range instanceof TypeImpl_string) {
      return getStringValue(feat);
    }
    
    if (range.getCode() == TypeSystemImpl.javaObjectTypeCode) {
      return CASImpl.serializeJavaObject(getJavaObjectValue(feat));
    }
    
    if (range.isRefType) {
      TOP ref = getFeatureValue(feat);
      return (ref == null) ? null : ref.toString();
    }
    
    Misc.internalError();
    return null;  // needed to avoid compile error
  }

  
  /* (non-Javadoc)
   * Supports "natural" compare order based on id values
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(FeatureStructure o) {
    return Integer.compare(this._id, o.id());
  }
  
  protected boolean _inSetSortedIndex() { return (flags & IN_SET_SORTED_INDEX) != 0;}
  protected void _setInSetSortedIndexed() { flags |= IN_SET_SORTED_INDEX; }
  /**
   * All callers of this must insure fs is not indexed in **Any** View
   */
  protected void _resetInSetSortedIndex() { flags &= ~IN_SET_SORTED_INDEX; }
  
  protected FeatureImpl _getFeatFromAdjOffset(int adjOffset, boolean isInInt) {
    return _typeImpl.getFeatureByAdjOffset(adjOffset, isInInt);
  }
  
  private int _getIntValueCommon(FeatureImpl feat) {
    return _intData[feat.getAdjustedOffset()];
  }

  private int _getIntValueCommon(int adjOffset) {
    return _intData[adjOffset];
  }

  private Object _getRefValueCommon(FeatureImpl feat) {
    return _refData[feat.getAdjustedOffset()];
  }
  
  private Object _getRefValueCommon(int adjOffset) {
    return _refData[adjOffset];
  }
   
  private void _setIntValueCommon(FeatureImpl fi, int v) {
    _intData[fi.getAdjustedOffset()] = v;
  }
  
  private void _setIntValueCommon(int adjOffset, int v) {
    _intData[adjOffset] = v;
  }

  private void _setRefValueCommon(FeatureImpl fi, Object v) {
    _refData[fi.getAdjustedOffset()] = v;
  }
  
  // used also for sofa string setting
  protected void _setRefValueCommonWj(FeatureImpl fi, Object v) {
    _refData[fi.getAdjustedOffset()] = v;
    _casView.maybeLogUpdate(this, fi);
  }

  /*************************************
   *  Validation checking
   *************************************/
  private void featureValidation(Feature feat) {   
    if (!(((TypeImpl) (feat.getDomain()) ).subsumes(_typeImpl))) {
      /* Feature "{0}" is not defined for type "{1}". */
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT, feat.getName(), _typeImpl.getName());
    }
  }
  
//  private void featureValidation(Feature feat, Object x) {
//    featureValidation(feat);
//    if (feat.getRange())
//  }
    
  private void featureValueValidation(Feature feat, Object v) {
    TypeImpl range = (TypeImpl)feat.getRange();
    if ((range.isArray() && !isOkArray(range, v)) ||
        (!range.isArray() && (!range.subsumesValue(v)))) {
      throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, feat.getName(), range.getName(), (v == null) ? "null" : v.getClass().getName());
    }
  }
    
  // called when range isArray() is true, only
  private boolean isOkArray(TypeImpl range, Object v) {
    if (v == null) {
      return true;
    }
    
    final int rangeTypeCode = range.getCode();

    /* The assignment is stricter than the Java rules - must match */
    switch (rangeTypeCode) {
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
    case TypeSystemImpl.fsArrayTypeCode:
      return v instanceof FSArray;
    }
    
    // it is possible that the array has a special type code corresponding to a type "someUserType"[]
    //   meaning an array of some user type.  UIMA implements these as instances of FSArray (I think)
    
    if (!(v instanceof FSArray)) { return false; }
    
    return true;
  }

  private void subStringRangeCheck(Feature feat, String v) {
    Type range = feat.getRange();
    if (range instanceof TypeImpl_stringSubtype) {
      if (v != null) { // null values always OK
        ((TypeImpl_stringSubtype)range).validateIsInAllowedValues(v);
      }
    }     
  }
  
}
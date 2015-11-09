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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for BooleanArray */
public final class BooleanArray extends TOP implements CommonPrimitiveArray, BooleanArrayFS {
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(BooleanArray.class);

  public final static int type = typeIndexID;

  /**
   * used to obtain reference to the _Type instance
   * 
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /* local data */
  private final boolean[] theArray;

 
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private BooleanArray() {
    theArray = null;
  }

  /**
   * Make a new BooleanArray of given size
   * @param jcas JCas reference
   * @param length of array
   */
  public BooleanArray(JCas jcas, int length) {
    super(jcas);
    theArray = new boolean[length];
  }
  
  /**
   * Called by generator 
   * @param c -
   * @param t -
   * @param l -
   */
  public BooleanArray(TypeImpl t, CASImpl c, int l) {
    super(t, c);
    theArray = new boolean[l];
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#get(int)
   */
  public boolean get(int i) {
    return theArray[i];
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#set(int , boolean)
   */
  public void set(int i, boolean v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#copyFromArray(boolean[], int, int, int)
   */
  public void copyFromArray(boolean[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#copyToArray(int, boolean[], int, int)
   */
  public void copyToArray(int srcPos, boolean[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#toArray()
   */
  public boolean[] toArray() {
    return theArray.clone();
  }

  /** return the size of the array */
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      dest[i + destPos] = Boolean.toString(theArray[i + srcPos]);
    }
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      theArray[i + destPos] = Boolean.parseBoolean(src[i + srcPos]);
    }
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    BooleanArray bv = (BooleanArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonPrimitiveArray#setArrayValueFromString(int, java.lang.String)
   */
  @Override
  public void setArrayValueFromString(int i, String v) {
    set(i, Boolean.parseBoolean(v));
  }
  
}

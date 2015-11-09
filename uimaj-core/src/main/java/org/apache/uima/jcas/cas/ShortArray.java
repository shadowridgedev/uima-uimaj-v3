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

import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for ShortArray */
public final class ShortArray extends TOP implements CommonPrimitiveArray, ShortArrayFS {
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(ShortArray.class);

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

  private final short[] theArray;
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private ShortArray() {
    theArray = null;
  }

  /**
   * Make a new ShortArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public ShortArray(JCas jcas, int length) {
    super(jcas);
    theArray = new short[length];
  }

  /**
   * used by generator
   * Make a new ShortArray of given size
   * @param c -
   * @param t - 
   * @param length The number of elements in the new array
   */
  public ShortArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    theArray = new short[length];
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#get(int)
   */
  public short get(int i) {
    return theArray[i];
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#set(int , short)
   */
  public void set(int i, short v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyFromArray(short[], int, int, int)
   */
  public void copyFromArray(short[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyToArray(int, short[], int, int)
   */
  public void copyToArray(int srcPos, short[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#toArray()
   */
  public short[] toArray() {
    return theArray.clone();
  }

  /** return the size of the array */
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      dest[i + destPos] = Short.toString(theArray[i + srcPos]);
    }
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, destPos, length);
    for (int i = 0; i < length; i++) {
      theArray[i + destPos] = Short.parseShort(src[i + srcPos]);
    }
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
  
  // internal use
  public short[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    ShortArray bv = (ShortArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
  }

  // used by deserializers
  public void setArrayValueFromString(int i, String v) {
    set(i, Short.parseShort(v));
  }
}

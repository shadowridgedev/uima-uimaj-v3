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

import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** The Java Class model corresponding to the Cas IntegerArray_JCasImpl type. */
public final class IntegerArray extends TOP implements CommonPrimitiveArray, IntArrayFS {
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(IntegerArray.class);

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
  
  private final int[] theArray;

  @SuppressWarnings("unused")
  private IntegerArray() { // never called. Here to disable default constructor
    theArray = null;
  }

  /**
   * Make a new IntegerArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */  
  public IntegerArray(JCas jcas, int length) {
    super(jcas);
    theArray = new int[length];
  }

  /**
   * used by generator
   * Make a new IntegerArray of given size
   * @param c -
   * @param t -
   * @param length the length of the array in bytes
   */
  public IntegerArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    theArray = new int[length];
  }

  /**
   * return the indexed value from the corresponding Cas IntegerArray_JCasImpl as an int.
   */
  public int get(int i) {
    return theArray[i];
  }

  /**
   * update the Cas, setting the indexed value to the passed in Java int value.
   */
  public void set(int i, int v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(int[], int, int, int)
   * 
   */
  public void copyFromArray(int[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, int[], int, int)
   */
  public void copyToArray(int srcPos, int[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#toArray()
   */
  public int[] toArray() {
    return theArray.clone();
  }

  /** return the size of the array */
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    _casView.checkArrayBounds(theArray.length, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Integer.toString(theArray[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    _casView.checkArrayBounds(theArray.length, srcOffset, length);
    for (int i = 0; i < length; i++) {
      theArray[i + destOffset] = Integer.parseInt(src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#toStringArray()
   */
  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
  
  // internal use only
  public int[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    IntegerArray bv = (IntegerArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
  }

}

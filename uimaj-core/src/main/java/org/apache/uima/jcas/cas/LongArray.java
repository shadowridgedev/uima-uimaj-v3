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

import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for LongArray */
public final class LongArray extends TOP implements LongArrayFS {
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(LongArray.class);

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

  // never called. Here to disable default constructor
  private LongArray() {
  }

 /* Internal - Constructor used by generator */
  public LongArray(int addr, TOP_Type type) {
    super(addr, type);
  }

  /**
   * Make a new LongArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public LongArray(JCas jcas, int length) {
    this(jcas.getLowLevelCas().ll_createLongArray(length), jcas.getType(typeIndexID));
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#get(int)
   */
  public long get(int i) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    return jcasType.ll_cas.ll_getLongArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#set(int , long)
   */
  public void set(int i, long v) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    jcasType.ll_cas.ll_setLongArrayValue(addr, i, v);
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyFromArray(long[], int, int, int)
   */
  public void copyFromArray(long[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setLongArrayValue(addr, i + destOffset, src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyToArray(int, long[], int, int)
   */
  public void copyToArray(int srcOffset, long[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = jcasType.ll_cas.ll_getLongArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#toArray()
   */
  public long[] toArray() {
    final int size = size();
    long[] outArray = new long[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /** return the size of the array */
  public int size() {
    return jcasType.casImpl.ll_getArraySize(addr);
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Long.toString(jcasType.ll_cas
              .ll_getLongArrayValue(addr, i + srcOffset));
    }
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas
              .ll_setLongArrayValue(addr, i + destOffset, Long.parseLong(src[i + srcOffset]));
    }
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
}

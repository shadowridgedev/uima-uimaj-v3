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

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyFSList extends FSList implements NonEmptyList {

  public final static int typeIndexID = JCasRegistry.register(NonEmptyFSList.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  public static final int _FI_head = TypeSystemImpl.getAdjustedFeatureOffset("head");
  public static final int _FI_tail = TypeSystemImpl.getAdjustedFeatureOffset("tail");

//  /* local data */
//  private TOP _F_head;
//  private FSList _F_tail;
  
  // Never called. Disable default constructor
  protected NonEmptyFSList() {
  }

  public NonEmptyFSList(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public NonEmptyFSList(TypeImpl t, CASImpl c) {
    super(t, c);
  }

  // *------------------*
  // * Feature: head
  /* getter for head * */
  public TOP getHead() { return _getFeatureValueNc(_FI_head); }

  /* setter for head * */
  public void setHead(TOP v) { _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_head, false), v); }

//  public void _setHeadNcNj(TOP v) { _F_head = v; }
  
  // *------------------*
  // * Feature: tail
  /* getter for tail * */
  public FSList getTail() { return (FSList) _getFeatureValueNc(_FI_tail); }

  /* setter for tail * */
  public void setTail(FSList v) { _setFeatureValueNcWj(_getFeatFromAdjOffset(_FI_tail, false), v); }
    
  public void setTail(CommonList v) {
    setTail((FSList) v);
  }
  
  public TOP getNthElement(int i) {
    return ((NonEmptyFSList)getNonEmptyNthNode(i)).getHead();
  }
  
}

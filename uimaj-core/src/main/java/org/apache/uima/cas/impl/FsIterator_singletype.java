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

import java.util.Comparator;
import java.util.ConcurrentModificationException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

public abstract class FsIterator_singletype<T extends FeatureStructure>
                    implements LowLevelIterator<T>, 
                               Comparable<FsIterator_singletype<T>> {

  private int modificationSnapshot; // to catch illegal modifications

//  /**
//   * This is a ref to the shared value in the FSIndexRepositoryImpl
//   * OR it may be null which means skip the checking (done for some internal routines
//   * which know they are not updating the index, and assume no other thread is)
//   */
//  final protected int[] detectIllegalIndexUpdates; // shared copy with Index Repository

  protected final TypeImpl ti;  
  
  /**
   * The generic type is FeatureStructure to allow comparing between
   * an instance of T and some other template type which can be a supertype of T, as long as
   * the keys are defined in both.
   */
  final protected Comparator<FeatureStructure> comparator;

  public FsIterator_singletype(TypeImpl ti, Comparator<FeatureStructure> comparator){
    this.comparator = comparator;
//    this.detectIllegalIndexUpdates = detectConcurrentMods;
    this.ti = ti;
//    resetConcurrentModification();  // can't do here, each subtype must finish it's initialization first
    // subtypes do moveToFirst after they finish initialization
  }

  protected abstract int getModificationCountFromIndex();
  
//  final protected <I extends FSIterator<T>> I checkConcurrentModification() {
//    if (modificationSnapshot != getModificationCountFromIndex()) {
////    if ((null != detectIllegalIndexUpdates) && (modificationSnapshot != detectIllegalIndexUpdates[typeCode])) {
//      throw new ConcurrentModificationException();
//    }
//    return (I) this;
//  }
  
  protected void maybeTraceCowUsingCopy(FsIndex_singletype<?> idx, CopyOnWriteIndexPart iteratorCopy) {
    if (CASImpl.traceCow) {
      if (idx.getCopyOnWriteIndexPart() != iteratorCopy) {
        idx.casImpl.traceCowCopyUse(idx);
      }
    }
  }
  
  protected void resetConcurrentModification() {  
    this.modificationSnapshot = // (null == this.detectIllegalIndexUpdates) ? 0 : this.detectIllegalIndexUpdates[typeCode];
                              getModificationCountFromIndex();
  }

  @Override
  public int compareTo(FsIterator_singletype<T> o) {
    if (comparator != null) {
      return comparator.compare(this.get(), o.get());
    } 
    return Integer.compare(this.get()._id(), o.get()._id());
  }
   
  @Override
  public abstract FsIterator_singletype<T> copy();
  
  @Override
  public String toString() {
    Type type = ti;
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(":").append(System.identityHashCode(this));
    sb.append(" over Type: ").append(type.getName()).append(":").append(ti.getCode());
    sb.append(", size: ").append(this.ll_indexSize());
    return sb.toString();
  }
  
}

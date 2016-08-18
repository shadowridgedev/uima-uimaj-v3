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

import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.ObjHashSet;

class FsIterator_bag<T extends FeatureStructure> extends FsIterator_singletype<T> {

  private ObjHashSet<T> bag;
  
  private int position = -1;  
  
  private boolean isGoingForward = true;

  final protected FsIndex_bag<T> fsBagIndex; // just an optimization, is == to fsLeafIndexImpl from super class, allows dispatch w/o casting

  FsIterator_bag(FsIndex_bag<T> fsBagIndex, TypeImpl ti) {
    super(ti, null);  // null: null comparator for bags
    this.fsBagIndex = fsBagIndex;  // need for copy()
    bag = (ObjHashSet<T>) fsBagIndex.getObjHashSet();
    resetConcurrentModification();
    moveToFirst();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return (position >= 0) && (position < bag.getCapacity());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#get()
   */
  @Override
  public T get() {
    checkConcurrentModification();
    if (isValid()) {
      return bag.get(position);
    }
    throw new NoSuchElementException();
  }

  public T getNvc() {
    checkConcurrentModification();
    return bag.get(position);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  @Override
  public void moveToFirst() {
    resetConcurrentModification();
    isGoingForward = true;
    position = (bag.size() == 0) ? -1 : bag.moveToNextFilled(0);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   *  If empty, make position -1 (invalid)
   */
  @Override
  public void moveToLast() {
    resetConcurrentModification();
    isGoingForward = false;
    position =  (bag.size() == 0) ? -1 : bag.moveToPreviousFilled(bag.getCapacity() -1);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  @Override
  public void moveToNext() {
    checkConcurrentModification(); 
    if (isValid()) {
      isGoingForward = true;
      position = bag.moveToNextFilled(++position);
    }
  }
  
  @Override
  public void moveToNextNvc() {
    checkConcurrentModification(); 
    isGoingForward = true;
    position = bag.moveToNextFilled(++position);
  }


  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  @Override
  public void moveToPrevious() {
    checkConcurrentModification();
    if (isValid()) {
      isGoingForward = false;
      position = bag.moveToPreviousFilled(--position);
    }
  }

  @Override
  public void moveToPreviousNvc() {
    checkConcurrentModification();
    isGoingForward = false;
    position = bag.moveToPreviousFilled(--position);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void moveTo(FeatureStructure fs) {
    resetConcurrentModification();
    position = bag.moveTo(fs);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FsIterator_bag<T> copy() {
    FsIterator_bag<T> copy = new FsIterator_bag<T>(this.fsBagIndex, this.ti);
    copyCommonSetup(copy);
    return copy;
  }
  
  protected void copyCommonSetup(FsIterator_bag<T> copy) {
    copy.position = position;
    copy.isGoingForward = isGoingForward;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */  @Override
  public int ll_indexSize() {
    return bag.size();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_getIndex()
   */
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return fsBagIndex;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIterator_singletype#getModificationCountFromIndex()
   */
  @Override
  protected int getModificationCountFromIndex() {
    return bag.getModificationCount();
  }

  
}


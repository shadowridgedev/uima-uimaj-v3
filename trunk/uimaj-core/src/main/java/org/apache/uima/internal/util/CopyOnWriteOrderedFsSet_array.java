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
package org.apache.uima.internal.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.impl.CopyOnWriteIndexPart;
import org.apache.uima.jcas.cas.TOP;

/**
 * implements OrderedFsSet_array partially, for iterator use
 */

public class CopyOnWriteOrderedFsSet_array implements CopyOnWriteIndexPart {
  
  private OrderedFsSet_array<TOP> set;
    
  final public int a_firstUsedslot;
  final public int a_nextFreeslot;
  final public OrderedFsSet_array<TOP> original;
  final private int original_size;
  
  public TOP[] a;  // derived from "set" above
   
  public CopyOnWriteOrderedFsSet_array(OrderedFsSet_array original) {
    this.set = original;    
    this.original = original;
//    this.comparatorNoTypeWithoutID = original.comparatorNoTypeWithoutID;
//    this.comparatorNoTypeWithID = original.comparatorNoTypeWithID;
    this.a_firstUsedslot = original.a_firstUsedslot;
    this.a_nextFreeslot = original.a_nextFreeslot;
    this.a = original.a;
    this.original_size = original.size();
  }
  
  /**
   * Called by index when about to make an update
   * This copy captures the state of things before the update happens
   */
  @Override
  public void makeReadOnlyCopy() {
    this.set = new OrderedFsSet_array<TOP>(set, true); // true = make read only copy
    this.a = set.a;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.CopyOnWriteIndexPart#isOriginal(java.lang.Object)
   */
  @Override
  public boolean isOriginal() {
    return set == original;
  }
  
  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return set.hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CopyOnWriteOrderedFsSet_array) {
      return set.equals(((CopyOnWriteOrderedFsSet_array)obj).set);  // set object equals
    }
    return false;
  }

  /**
   * @see OrderedFsSet_array#size()
   * @return the size of this version of the index (maybe not the current index size)
   */
  final public int size() {
    return original_size;
  }

//  /**
//   * @return the modification count
//   */
//  public int getModificationCount() {
//    return set.getModificationCount();
//  }

  /**
   * @see OrderedFsSet_array#toString()
   */
  @Override
  public String toString() {
    return set.toString();
  }
    
  public OrderedFsSet_array<TOP> getOfsa() {
    return set;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<TOP> iterator() {
    return new Iterator<TOP>() {

      int pos = a_firstUsedslot;
      
      @Override
      public boolean hasNext() {
        return pos >= 0 && pos < a_nextFreeslot;
      }

      @Override
      public TOP next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return a[pos++];
      }      
    };
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.CopyOnWriteIndexPart#copyToArray(org.apache.uima.jcas.cas.TOP[], int)
   */
  @Override
  public int copyToArray(TOP[] target, int startingIndexInTarget) {
    System.arraycopy(a, a_firstUsedslot, target, startingIndexInTarget, size());
    return startingIndexInTarget + size();
  }

}

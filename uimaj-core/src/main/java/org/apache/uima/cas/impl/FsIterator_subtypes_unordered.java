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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * This class has an instance created temporarily to reuse the 
 * computation of the iterator array, for use by the FsIterator_aggregation_common.
 * @param <T> the type of the iterator
 */
public class FsIterator_subtypes_unordered<T extends FeatureStructure> extends FsIterator_subtypes_list<T> {
 
  public FsIterator_subtypes_unordered(FsIndex_iicp<T> iicp) {
    super(iicp);
  } 
  
  // these methods are never called but are needed to allow creation of the temporary instance of this.
  
  @Override
  public boolean isValid() {throw new UnsupportedOperationException();}
  @Override
  public T get() throws NoSuchElementException {throw new UnsupportedOperationException();}
  @Override
  public T getNvc() {throw new UnsupportedOperationException();}
  @Override
  public void moveToNext() {throw new UnsupportedOperationException();}
  @Override
  public void moveToNextNvc() {throw new UnsupportedOperationException();}
  @Override
  public void moveToPrevious() {throw new UnsupportedOperationException();}
  @Override
  public void moveToFirst() {}  // can't throw, should be a no-op.
  @Override
  public void moveToLast() {throw new UnsupportedOperationException();}
  @Override
  public void moveTo(FeatureStructure fs) {throw new UnsupportedOperationException();}
  @Override
  public FSIterator<T> copy() {throw new UnsupportedOperationException();}
  @Override
  public boolean hasNext() {throw new UnsupportedOperationException();}
  @Override
  public T next() {throw new UnsupportedOperationException();}
}

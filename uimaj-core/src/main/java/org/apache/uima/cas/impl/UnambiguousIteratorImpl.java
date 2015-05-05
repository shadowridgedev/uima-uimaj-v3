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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Implementation of the unambiguous iterators.
 * 
 * <p>
 * Warning: this implementation creates a copy of the collection, so changes in the underlying
 * collection are not reflected by this iterator.
 * 
 * 
 */
public class UnambiguousIteratorImpl<T extends AnnotationFS> extends FSIteratorImplBase<T> {

  private ArrayList<T> list;

  private int pos;

  private Comparator<AnnotationFS> annotationComparator;

  private UnambiguousIteratorImpl() {
    super();
    this.pos = 0;
  }

  
  public UnambiguousIteratorImpl(FSIterator<T> it) {
    this();
    this.list = new ArrayList<T>();
    it.moveToFirst();
    if (!it.isValid()) {
      return;
    }
    T current, next;
    current = it.get();
    this.list.add(current);
    it.moveToNext();
    while (it.isValid()) {
      next = it.get();
      if (current.getEnd() <= next.getBegin()) {
        current = next;
        this.list.add(current);
      }
      it.moveToNext();
    }
  }

  
  public UnambiguousIteratorImpl(FSIterator<T> it, final int start, final int end, final boolean strict) {
    super();
    initUnambiguousSubiterator(it, start, end, strict);
  }

  private void initUnambiguousSubiterator(FSIterator<T> it, final int start, final int end,
          final boolean strict) {
    this.list = new ArrayList<T>();
    it.moveToFirst();
    // Skip annotations with begin positions before the given start
    // position.
    while (it.isValid() && start > it.get().getBegin()) {
      it.moveToNext();
    }
    // Add annotations.
    T current, next;
    if (!it.isValid()) {
      this.pos = 0;
      return;
    }
    current = it.get();
    this.list.add(current);
    it.moveToNext();
    while (it.isValid()) {
      next = it.get();
      // If the next annotation overlaps, skip it.
      if (next.getBegin() < current.getEnd()) {
        it.moveToNext();
        continue;
      }
      // If we're past the end, stop.
      if (next.getBegin() > end) {
        break;
      }
      // We have an annotation that's within the boundaries and doesn't
      // overlap
      // with the previous annotation. We add this annotation if we're not
      // strict, or the end position is within the limits.
      if (!strict || current.getEnd() <= end) {
        current = next;
        this.list.add(current);
      }
      it.moveToNext();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  public boolean isValid() {
    return (this.pos >= 0) && (this.pos < this.list.size());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#get()
   */
  public T get() throws NoSuchElementException {
    if (isValid()) {
      return this.list.get(this.pos);
    }
    throw new NoSuchElementException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  public void moveToNext() {
    ++this.pos;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  public void moveToPrevious() {
    --this.pos;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  public void moveToFirst() {
    this.pos = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  public void moveToLast() {
    this.pos = this.list.size() - 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  public void moveTo(FeatureStructure fs) {
    final int found = Collections.binarySearch(this.list, (AnnotationFS) fs, getAnnotationComparator(fs));
    if (found >= 0) {
      this.pos = found;
    } else {
      this.pos = (-found) - 1;
    }
  }
  
  private final Comparator<AnnotationFS> getAnnotationComparator(FeatureStructure fs) {
    if (this.annotationComparator == null) {
      this.annotationComparator = 
          ((FSIndexRepositoryImpl)(fs.getCAS().getIndexRepository())).getAnnotationFsComparator(); 
    }
    return this.annotationComparator;
  }



  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  public FSIterator<T> copy() {
    UnambiguousIteratorImpl<T> copy = new UnambiguousIteratorImpl<T>();
    copy.list = this.list;
    copy.pos = this.pos;
    return copy;
  }

}

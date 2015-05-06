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
 * Subiterator implementation.
 */
public class Subiterator<T extends AnnotationFS> extends FSIteratorImplBase<T> {

  private ArrayList<T> list;

  private int pos;

  private Comparator<AnnotationFS> annotationComparator = null;

  private Subiterator() {
    super();
    this.list = new ArrayList<T>();
    this.pos = 0;
  }

  /**
   * Create a disambiguated iterator.  No concurrent modification exception checking is done.
   * 
   * @param it
   *          The iterator to be disambiguated.
   */
  Subiterator(FSIterator<T> it) {
    this();
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

  
  Subiterator(FSIterator<T> it, AnnotationFS annot, final boolean ambiguous, final boolean strict) {
    this();
    if (ambiguous) {
      
      initAmbiguousSubiterator(it, annot, strict);
    } else {
      initUnambiguousSubiterator(it, annot, strict);
    }
  }

  // makes an extra copy of the items
  private void initAmbiguousSubiterator(FSIterator<T> it, AnnotationFS annot, final boolean strict) {
    final int start = annot.getBegin();
    final int end = annot.getEnd();
    it.moveTo(annot);  // to "earliest" equal, or if none are equal, to the one just later than annot
    
    // This is a little silly, it skips 1 of possibly many indexed annotations if the earliest one is "equal"
    //    (just means matching the keys) to the control annot  4/2015
    if (it.isValid() && it.get().equals(annot)) {
      it.moveToNext();
    }
    // Skip annotations whose start is before the start parameter.
    // should never have any???
    while (it.isValid() && it.get().getBegin() < start) {
      it.moveToNext();
    }
    T current;
    while (it.isValid()) {
      current = it.get();
      // If the start of the current annotation is past the end parameter,
      // we're done.
      if (current.getBegin() > end) {
        break;
      }
      it.moveToNext();
      if (strict && current.getEnd() > end) {
        continue;
      }
      this.list.add(current);
    }
  }

  private void initUnambiguousSubiterator(FSIterator<T> it, AnnotationFS annot, final boolean strict) {
    final int start = annot.getBegin();
    final int end = annot.getEnd();
    it.moveTo(annot);
    
    if (it.isValid() && it.get().equals(annot)) {
      it.moveToNext();
    }
    if (!it.isValid()) {
      return;
    }
    annot = it.get();
    this.list = new ArrayList<T>();
    // Skip annotations with begin positions before the given start
    // position.
    while (it.isValid() && ((start > annot.getBegin()) || (strict && annot.getEnd() > end))) {
      it.moveToNext();
    }
    // Add annotations.
    if (!it.isValid()) {
      return;
    }
    T current = null;
    while (it.isValid()) {
      final T next = it.get();
      // If the next annotation overlaps, skip it. Don't check while there is no "current" yet.
      if ((current != null) && (next.getBegin() < current.getEnd())) {
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
      if (!strict || next.getEnd() <= end) {
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
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  public void moveTo(FeatureStructure fs) {
    final Comparator<AnnotationFS> comparator = getAnnotationComparator(fs);
    pos = Collections.binarySearch(this.list, (AnnotationFS) fs, comparator);
    if (pos >= 0) {
      if (!isValid()) {
        return;
      }
      T foundFs = get();
      // Go back until we find a FS that is really smaller
      while (true) {
        moveToPrevious();
        if (isValid()) {
          if (comparator.compare(get(), foundFs) != 0) {
            moveToNext(); // go back
            break;
          }
        } else {
          moveToFirst();  // went to before first, so go back to 1st
          break;
        }
      }       
      return;
    } else {
      pos = (-pos) - 1;
      return;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  public FSIterator<T> copy() {
    Subiterator<T> copy = new Subiterator<T>();
    copy.list = this.list;
    copy.pos = this.pos;
    return copy;
  }

}

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

package org.apache.uima.cas;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Collection of builder style methods to specify selection of FSs from indexes
 * Comment codes:
 *   AI = implies AnnotationIndex
 *   Ordered = implies an ordered index not necessarily AnnotationIndex
 *   BI = bounded iterator (boundedBy or bounding)
 */
public interface SelectFSs<T extends FeatureStructure> extends Iterable<T>, Stream<T> {
  
  // Stream and Iterable both define forEach, with Iterable supplying a default.
  @Override
  default void forEach(Consumer<? super T> action) {
    Iterable.super.forEach(action);
  }
  
  
  
//  // If not specified, defaults to all FSs (unordered) unless AnnotationIndex implied
//    // Methods take their generic type from the variable to which they are assigned except for
//    // index(class) which takes it from its argument.
//  <N extends FeatureStructure> SelectFSs<N> index(String indexName);  
//  <N extends FeatureStructure> SelectFSs<N> index(FSIndex<N> index);
//
//  // If not specified defaults to the index's uppermost type.
//  // Methods take their generic type from the variable to which they are assigned except for
//  // type(class) which takes it from its argument.
//  <N extends FeatureStructure> SelectFSs<N> type(Type uimaType);
//  <N extends FeatureStructure> SelectFSs<N> type(String fullyQualifiedTypeName);
//  <N extends FeatureStructure> SelectFSs<N> type(int jcasClass_dot_type);
//  <N extends FeatureStructure> SelectFSs<N> type(Class<N> jcasClass_dot_class);
    
//  SelectFSs<T> shift(int amount); // incorporated into startAt 
  
  // ---------------------------------
  // boolean operations
  // ---------------------------------

//  SelectFSs<T> matchType();      // exact type match (no subtypes)
//  SelectFSs<T> matchType(boolean matchType); // exact type match (no subtypes)
  
  // only for AnnotationIndex
  /**
   * Specify that type priority should be included when comparing two Feature Structures while positioning an iterator
   * <p>
   * Default is to not include type priority.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> typePriority();
  
  /**
   * Specify that type priority should or should not be included when comparing two Feature Structures while positioning an iterator
   * <p>
   * Default is to not include type priority.
   * @param typePriority if true says to include the type priority
   * @return the updated SelectFSs object
   */
  SelectFSs<T> typePriority(boolean typePriority);

  /**
   * Only used when typePriority false, and only for Annotation Indexes
   * <p>
   * For annotation index, specifies that after a moveTo(FeatureStructure) operation has
   *   found a feature structure that compares equal with the argument, 
   *   the position is adjusted downwards while the element at the position is equal to the argument,
   *   where the equal includes the type.
   * <p>
   * Default is to not include the type.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> positionUsesType();           // ignored if not ordered index

  /**
   * Only used when typePriority false, and only for Annotation Indexes
   * <p>
   * For annotation index, specifies that after a moveTo(FeatureStructure) operation has
   *   found a feature structure that compares equal with the argument, 
   *   the position is adjusted downwards while the element at the position is equal to the argument,
   *   where the equal includes or doesn't include the type.
   * <p>
   * Default is to not include the type.
   * @param positionUsesType true if the adjustment should require the types be the same.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> positionUsesType(boolean positionUsesType); // ignored if not ordered index
  
  // Filters while iterating over Annotations
  
  /**
   * Meaningful only for Annotation Indexes, specifies that iteration should return 
   * only annotations which don't overlap with each other.  Also known as "unambiguous".
   * <p>
   * Default is to not have this filter.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nonOverlapping();  // AI known as unambiguous
  /**
   * Meaningful only for Annotation Indexes, specifies that iteration should or should not return 
   * only annotations which don't overlap with each other.  Also known as "unambiguous".
   * <p>
   * Default is to not have this filter.
   * @param nonOverlapping true to specify filtering for only non-overlapping annotations.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nonOverlapping(boolean nonOverlapping); // AI
  
  /**
   * Meaningful only for coveredBy, includes annotations where the end exceeds the bounding annotation's end.
   * <p>
   * Default is to NOT include annotations whose end exceeds the bounding annotation's end.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> includeAnnotationsWithEndBeyondBounds();  // AI known as "strict"
  /**
   * Meaningful only for coveredBy, includes or filters out annotations where the end exceeds the bounding annotation's end.
   * <p>
   * Default is to NOT include annotations whose end exceeds the bounding annotation's end.
   * @param includeAnnotationsWithEndBeyondBounds false to filter out annotations whose end exceeds the bounding annotation's end
   * @return the updated SelectFSs object
   */
  SelectFSs<T> includeAnnotationsWithEndBeyondBounds(boolean includeAnnotationsWithEndBeyondBounds); // AI

  /**
   * Meaningful only for coveredBy: if true, then returned annotations are compared equal to 
   * the bounding annotation, and if equal, they are skipped.
   * <p>
   * Default is to use feature structure identity comparison (same id()s), not equals, when 
   * doing the test to see if an annotation should be skipped.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> useAnnotationEquals();                 
  /**
   * Meaningful only for coveredBy: if true, then returned annotations are compared 
   * to the bounding annotation
   * using the specified kind of equal comparison,  
   * and if equal, they are skipped.
   * <p>
   * Default is to use feature structure identity comparison (same id()s), not equals, when 
   * doing the test to see if an annotation should be skipped.
   * @param useAnnotationEquals if true, use equals, if false, use id() ==.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> useAnnotationEquals(boolean useAnnotationEquals);
  
  // Miscellaneous
  /**
   * Extend the selection to be over all the CAS views, not just a single view.
   * <p>
   * Default is that the selection is just for one CAS view
   * @return the updated SelectFSs object
   */
  SelectFSs<T> allViews();
  /**
   * Extend or not extend the selection to be over all the CAS views, not just a single view.
   * <p>
   * Default is that the selection is just for one CAS view
   * @param allViews true to extend the selection.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> allViews(boolean allViews);
  
  /**
   * Applies to the various argument forms of the get method.
   * Indicates that a null value should not throw an exception.
   * <p>
   * Default: null is not OK as a value 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nullOK();  
  /**
   * Applies to the various argument forms of the get method.
   * Indicates that a null value should or should not throw an exception.
   * <p>
   * Default: null is not OK as a value 
   * @param nullOk true if null is an ok value.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> nullOK(boolean nullOk);  // applies to get()
    
  /**
   * Specifies that order is not required while iterating over an otherwise ordered index.
   * This can be a performance boost for hierarchically nested types.
   * <p>
   * Default: order is required by default, when iterating over an ordered index. 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> orderNotNeeded();                  // ignored if not ordered index
  /**
   * Specifies that order is or is not required while iterating over an otherwise ordered index.
   * This can be a performance boost for hierarchically nested types.
   * <p>
   * Default: order is required by default, when iterating over an ordered index.
   * @param unordered true means order is not needed. 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> orderNotNeeded(boolean unordered); // ignored if not ordered index
  
  /**
   * Specifies that the iteration should run in reverse order from normal.
   * Note that this does not compose; 
   * two calls to this will still result in the iteration running in reverse order.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> backwards();                  // ignored if not ordered index
  /**
   * Specifies that the iteration should run in the normal or reverse order.
   * Note that this does not compose. 
   * @param backwards true to run in reverse order
   * @return the updated SelectFSs object
   */
  SelectFSs<T> backwards(boolean backwards); // ignored if not ordered index

//  SelectFSs<T> noSubtypes();
//  SelectFSs<T> noSubtypes(boolean noSubtypes);

  
  // ---------------------------------
  // starting position specification
  // 
  // Variations, controlled by: 
  //   * typePriority
  //   * positionUsesType
  //   
  // The positional specs imply starting at the 
  //   - left-most (if multiple) FS at that position, or
  //   - if no FS at the position, the next higher FS
  //   - if !typePriority, equal test is only begin/end 
  //     -- types ignored or not depending on positionUsesType 
  //    
  // shifts, if any, occur afterwards
  //   - can be positive or negative
  // ---------------------------------
  /**
   * Starting Position specification - Shifts the normal start position by the shiftAmount, which may be negative.
   * Repeated calls to this just replaces the requested shift amount; a single shift only occurs when a 
   * result is obtained.
   * @param shiftAmount the amount to shift; this many Feature Structures which 
   *                      normally would be returned are instead skipped.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> shifted(int shiftAmount); 
  
  /**
   * Starting Position specification - For ordered sources, specifies which FS to start at. 
   * @param fs a Feature Structure specifying a starting position.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(TOP fs);  // Ordered
  /**
   * Starting Position specification - For Annotation Indexes, specifies which FS to start at. 
   * @param begin the begin bound
   * @param end the end bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(int begin, int end);   // AI
  /**
   * Starting Position specification - A combination of startAt followed by a shift
   * @param fs a Feature Structure specifying a starting position.
   * @param shift the amount to shift; this many Feature Structures which 
   *               normally would be returned are instead skipped.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(TOP fs, int shift);        // Ordered
  /**
   * Starting Position specification - A combination of startAt followed by a shift
   * @param begin the begin bound
   * @param end the end bound
   * @param shift the amount to shift; this many Feature Structures which 
   *               normally would be returned are instead skipped.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> startAt(int begin, int end, int shift);   // AI
    
  /**
   * Limits the number of Feature Structures returned by this select
   * @param n the maximum number of feature structures returned.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> limit(int n); 
  // ---------------------------------
  // subselection based on bounds
  //   - uses 
  //     -- typePriority, 
  //     -- positionUsesType, 
  //     -- useAnnotationEquals
  // ---------------------------------
  /**
   * Subselection - specifies selecting Feature Structures having the same begin and end
   *   - influenced by typePriority, positionUsesType, and useAnnotationEquals
   * @param fs specifies the bounds.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> at(AnnotationFS fs);  // AI
  /**
   * Subselection - specifies selecting Feature Structures having the same begin and end
   * @param begin the begin bound
   * @param end  the end bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> at(int begin, int end);  // AI
  /**
   * Subselection - specifies selecting Feature Structures 
   *   starting (and maybe ending) within a bounding Feature Structure
   *   - influenced by typePriority, positionUsesType, useAnnotationEquals,
   *     includeAnnotationsWithEndBeyondBounds
   * @param fs specifies the bounds.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> coveredBy(AnnotationFS fs);       // AI
  /**
   * Subselection - specifies selecting Feature Structures 
   *   starting (and maybe ending) within a bounding Feature Structure
   * @param begin the begin bound
   * @param end  the end bound
   * @return the updated SelectFSs object
   */
  SelectFSs<T> coveredBy(int begin, int end);       // AI
  /**
   * Subselection - specifies selecting Feature Structures 
   *   starting before or equal to bounding Feature Structure
   *   and ending at or beyond the bounding Feature Structure
   *   - influenced by typePriority, positionUsesType, useAnnotationEquals
   * @param fs specifies the bounds.
   * @return the updated SelectFSs object
   */  
  SelectFSs<T> covering(AnnotationFS fs);      // AI
  /**
   * Subselection - specifies selecting Feature Structures 
   *   starting before or equal to bounding Feature Structure's begin
   *   and ending at or beyond the bounding Feature Structure's end
   * @param begin the begin bound
   * @param end  the end bound
   * @return the updated SelectFSs object
   */  
  SelectFSs<T> covering(int begin, int end);      // AI
  
  /**
   * Subselection - specifies selecting Feature Structures 
   *   which lie between two annotations.
   *   A bounding Annotation is constructed whose begin
   *   is the end of fs1, and whose end is the begin of fs2.
   * <p> 
   * If fs1 &gt; fs2, they are swapped, and the selected values are returned in reverse order.
   * 
   * @param fs1 the beginning bound
   * @param fs2 the ending bound
   * @return the updated SelectFSs object
   */  
  SelectFSs<T> between(AnnotationFS fs1, AnnotationFS fs2);  // AI implies a coveredBy style
 
  /* ---------------------------------
  * Semantics: 
  *   - following uimaFIT
  *   - must be annotation subtype, annotation index
  *   - following: move to first fs where begin > pos.end
  *   - preceding: move to first fs where end < pos.begin
  *   
  *   - return the limit() or all
  *   - for preceding, return in forward order (unless backward is specified)
  *   - for preceding, skips FSs whose end >= begin (adjusted by offset)
  * ---------------------------------*/
  /**
   * For AnnotationIndex, position to first Annotation 
   * whose begin &gt; fs.getEnd();
   * @param annotation the Annotation to follow
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(Annotation annotation);
  /**
   * For AnnotationIndex, position to first Annotation whose begin &gt; fs.getEnd();
   * @param position start following this position
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(int position);
  /**
   * For AnnotationIndex, position to first Annotation whose begin &gt; fs.getEnd()
   *   and then adjust position by the offset
   * @param annotation start following this Annotation, adjusted for the offset
   * @param offset positive or negative shift amount to adjust starting position 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(Annotation annotation, int offset);
  /**
   * For AnnotationIndex, position to first Annotation whose begin &gt; position
   *   and then adjust position by the offset.
   * @param position start following this position, adjusted for the offset
   * @param offset positive or negative shift amount to adjust starting position 
   * @return the updated SelectFSs object
   */
  SelectFSs<T> following(int position, int offset);

  /**
   * For AnnotationIndex, set up a selection that will proceed backwards, 
   * starting at the first Annotation whose end &lt;= fs.getBegin().
   * Annotations whose end &gt; fs.getBegin() are skipped.
   * @param annotation the Annotation to use as the position to start before.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(Annotation annotation);
  /**
   * For AnnotationIndex, set up a selection that will proceed backwards, 
   * starting at the first Annotation whose end &lt;= position.
   * Annotations whose end &gt; position are skipped.
   * @param position the position to start before.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(int position);
  /**
   * For AnnotationIndex, set up a selection that will proceed backwards, 
   * starting at the first Annotation whose end &lt;= fs.getBegin(),
   * after adjusting by offset items.
   * Annotations whose end &gt; fs.getBegin() are skipped (including during the offset positioning)
   * @param annotation the Annotation to use as the position to start before.
   * @param offset the offset adjustment, positive or negative.  Positive moves backwards.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(Annotation annotation, int offset);
  /**
   * For AnnotationIndex, set up a selection that will proceed backwards, 
   * starting at the first Annotation whose end &lt;= position.
   * after adjusting by offset items.
   * Annotations whose end &gt; fs.getBegin() are skipped (including during the offset positioning)
   * @param position the position to start before.
   * @param offset the offset adjustment, positive or negative.  Positive moves backwards.
   * @return the updated SelectFSs object
   */
  SelectFSs<T> preceding(int position, int offset);
  // ---------------------------------
  // terminal operations
  // returning other than SelectFSs
  // 
  // ---------------------------------
  /**
   * @return an FSIterator over the selection.  The iterator is set up depending on 
   *         preceding configuration calls to this SelectFSs instance.
   */
  FSIterator<T> fsIterator();
//  Iterator<T> iterator();  // inherited, not needed here
  /**
   * @param <N> the generic type argument of the elements of the list
   * @return a List object whose elements represent the selection.
   */
  <N extends T> List<N> asList();
  /**
   * @param clazz the class of the type of the elements
   * @param <N> the generic type argument of the elements of the array
   * @return a Array object representation of the elements of the selection.
   */
  <N extends T> N[] asArray(Class<N> clazz);
//  Spliterator<T> spliterator(); // inherited, not needed here 
  
  // returning one item
  
  /**
   * Get the first element or null if empty or the element at the first position is null. 
   * if nullOK is false, then throws CASRuntimeException if null would have been returned. 
   * @return first element or null if empty
   * @throws CASRuntimeException conditioned on nullOK == false, and null being returned or the selection is empty.
   */
  T get();          // returns first element or null if empty (unless nullOK(false) specified)
  /**
   * @return first element, which must be not null
   * @throws CASRuntimeException if element is null or 
   *          if there is more than 1 element in the selection,
   *          or if the selection is empty
   */
  T single();       // throws if not exactly 1 element, throws if null
  /**
   * @return first element, which may be null, or null if selection is empty.
   * @throws CASRuntimeException if there is more than 1 element in the selection.
   */
  T singleOrNull(); // throws if more than 1 element, returns single or null
   // next are positioning alternatives
   // get(...) throws if null (unless nullOK specified)
  /**
   * Get the offset element or null if empty or the offset went outside the
   *    the selected elements.
   * <p> 
   * If nullOK is false, then throws CASRuntimeException if null would have been returned, or 
   *   the selection is empty, or doesn't have enough elements to satisfy the positioning. 
   * @param offset the offset adjustment, positive or negative.
   * @return the selected element or null
   * @throws CASRuntimeException conditioned on nullOK == false, and null being returned 
   *                   or the selection is empty,
   *                   or the offset positioning going outside the elements in the selection.
   */
  T get(int offset);          // returns first element or null if empty after positioning
  /**
   * Get the offset element or null if empty or the offset went outside the
   *    the selected elements.
   * <p>
   * If, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive, reverse direction if offset is negative)
   *   then throw an exception.
   * <p>
   * If nullOK is false, then throws CASRuntimeException if null would have been returned, or 
   *   the selection is empty, or doesn't have enough elements to satisfy the positioning. 
   * @param offset the offset adjustment, positive or negative.
   * @return the selected element or null
   * @throws CASRuntimeException if, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive, reverse direction if offset is negative)
   *   or (conditioned on nullOK == false) null being returned 
   *                   or the selection is empty,
   *                   or the offset positioning going outside the elements in the selection.
   */
  T single(int offset);       // throws if not exactly 1 element
  /**
   * Get the offset element or null if empty or the offset went outside the
   *    the selected elements.
   * <p>
   * If, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive, reverse direction if offset is negative)
   *   then throw an exception.
   * <p>
   * @param offset the offset adjustment, positive or negative.
   * @return the selected element or null
   * @throws CASRuntimeException if, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive, reverse direction if offset is negative)
   */
  T singleOrNull(int offset); // throws if more than 1 element, returns single or null  
  /**
   * Positions to the fs using moveTo(fs).
   * Get the element at that position or null if empty or the element at that position is null. 
   * if nullOK is false, then throws CASRuntimeException if null would have been returned. 
   * @param fs the positioning Feature Structure 
   * @return first element or null if empty
   * @throws CASRuntimeException (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T get(TOP fs);          // returns first element or null if empty after positioning
  /**
   * Positions to the fs using moveTo(fs).
   * Get the element at that position or null if empty or the element at that position is null. 
   * if nullOK is false, then throws CASRuntimeException if null would have been returned. 
   * @param fs the positioning Feature Structure 
   * @return first element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element following the one being returned 
   *     or (conditioned on nullOK == false) and null being returned or the selection is empty.
   */
  T single(TOP fs);       // throws if not exactly 1 element
  /**
   * Positions to the fs using moveTo(fs).
   * Get the element at that position or null if empty or the element at that position is null. 
   * @param fs the positioning Feature Structure 
   * @return first element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element following the one being returned 
   */
  T singleOrNull(TOP fs); // throws if more than 1 element, returns single or null
  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset).
   * Gets the element at that position or null if empty or the element at that position is null. 
   * if nullOK is false, then throws CASRuntimeException if null would have been returned. 
   * @param fs where to move to
   * @param offset the offset move after positioning to fs, may be 0 or positive or negative
   * @return the selected element or null if empty
   * @throws CASRuntimeException (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T get(TOP fs, int offset);          // returns first element or null if empty
  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset).
   * Gets the element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive or 0, reverse direction if offset is negative)
   *   then throw an exception.
   * <p>
   * If nullOK is false, then throws CASRuntimeException if null would have been returned.
   * @param fs the positioning Feature Structure 
   * @param offset the offset adjustment, positive or negative.
   * @return the selected element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element following the one being returned
   *     or (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T single(TOP fs, int offset);       // throws if not exactly 1 element
  /**
   * Positions to the fs using moveTo(fs), followed by a shifted(offset).
   * Gets the element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive or 0, reverse direction if offset is negative)
   *   then throw an exception.
   * <p>
   * @param fs the positioning Feature Structure 
   * @param offset the offset adjustment, positive or negative.
   * @return the selected element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element next to the one being returned 
   */
  T singleOrNull(TOP fs, int offset); // throws if more than 1 element, returns single or null
  /**
   * Position using a temporary Annotation with its begin and end set to the arguments.
   * Gets the element at that position or null if empty or the element at that position is null.
   * if nullOK is false, then throws CASRuntimeException if null would have been returned. 
   * @param begin the begin position of the temporary Annotation
   * @param end the end position of the temporary Annotation 
   * @return the selected element or null if empty
   * @throws CASRuntimeException conditioned on nullOK == false, and null being returned or the selection is empty.
   */
  T get(int begin, int end);          // returns first element or null if empty
  /**
   * Position using a temporary Annotation with its begin and end set to the arguments.
   * Gets the element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element following the one being returned 
   *   then throw an exception.
   * <p>
   * if nullOK is false, then throws CASRuntimeException if null would have been returned. 
   * @param begin the begin position of the temporary Annotation
   * @param end the end position of the temporary Annotation 
   * @return the selected element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element following the one being returned
   *             or (conditioned on nullOK == false) null being returned or the selection is empty.
   */
  T single(int begin, int end);       // throws if not exactly 1 element
  /**
   * Position using a temporary Annotation with its begin and end set to the arguments.
   * Gets the element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element following the one being returned 
   *   then throw an exception.
   * @param begin the begin position of the temporary Annotation
   * @param end the end position of the temporary Annotation 
   * @return the selected element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element following the one being returned
   */
  T singleOrNull(int begin, int end); // throws if more than 1 element, returns single or null
  /**
   * Position using a temporary Annotation with its begin and end set to the arguments,
   * followed by shifted(offset).
   * Gets the element at that position or null if empty or the element at that position is null.
   * <p>
   * @param begin the begin position of the temporary Annotation
   * @param end the end position of the temporary Annotation
   * @param offset the amount (positive or negative or 0) passed as an argument to shifted(int) 
   * @return the selected element or null if empty
   * @throws CASRuntimeException (conditioned on nullOK == false) if null being returned or the selection is empty.
   */
  T get(int begin, int end, int offset);          // returns first element or null if empty
  /**
   * Position using a temporary Annotation with its begin and end set to the arguments,
   * followed by shifted(offset).
   * Gets the element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive or 0, reverse direction if offset is negative)
   *   then throw an exception.
   * @param begin the begin position of the temporary Annotation
   * @param end the end position of the temporary Annotation
   * @param offset the amount (positive or negative or 0) passed as an argument to shifted(int) 
   * @return the selected element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element next to the one being returned
   *        or (conditioned on nullOK == false) if null being returned or the selection is empty.
   */
  T single(int begin, int end, int offset);       // throws if not exactly 1 element
  /**
   * Position using a temporary Annotation with its begin and end set to the arguments,
   * followed by shifted(offset).
   * Gets the element at that position or null if empty or the element at that position is null.
   * <p>
   * If, after positioning, there is another element next to the one being returned 
   *   (in the forward direction if offset is positive or 0, reverse direction if offset is negative)
   *   then throw an exception.
   * @param begin the begin position of the temporary Annotation
   * @param end the end position of the temporary Annotation
   * @param offset the amount (positive or negative or 0) passed as an argument to shifted(int) 
   * @return the selected element or null if empty
   * @throws CASRuntimeException if, after positioning, there is another element next to the one being returned
   */
  T singleOrNull(int begin, int end, int offset); // throws if more than 1 element, returns single or null
  
  
  @Override
  default Spliterator<T> spliterator() {
    // TODO Auto-generated method stub
    return Iterable.super.spliterator();
  }



  /**
   * Use this static method to capture the generic argument  
   * @param index - the index to select over as a source
   * @param <U> generic type of index
   * @param <V> generic type of returned select
   * @return - a SelectFSs instance
   */
  
  static <U extends FeatureStructure, V extends U> SelectFSs<V> select(FSIndex<U> index) {
    return index.select();    
  } 
  
//  /**
//   * DON'T USE THIS, use index.select(XXX.class) instead
//   * @param index the index to use
//   * @param clazz the JCas class
//   * @return a select instance for this index and type
//   */
//  static <U extends FeatureStructure, V extends U> SelectFSs<V> sselect(FSIndex<U> index, Class<V> clazz) {
//    return index.select(clazz);
//  }
 

}

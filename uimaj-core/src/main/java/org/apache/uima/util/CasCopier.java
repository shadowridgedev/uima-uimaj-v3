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
package org.apache.uima.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Utility class for doing deep copies of FeatureStructures from one CAS to another. To handle cases
 * where the source CAS has multiple references to the same FS, you can create one instance of
 * CasCopier and use it to copy multiple FeatureStructures. The CasCopier will remember previously
 * copied FeatureStructures, so if you later copy another FS that has a reference to a previously
 * copied FS, it will not duplicate the multiply-referenced FS.
 * 
 */
public class CasCopier {
  
  private final CAS mOriginalSrcCasView;
  private final CAS mOriginalTgtCasView;
  /**
   * The source view name - may be null if the view is of the base CAS
   */
  private String mSrcCasViewName;
  /**
   * The target view name - not used unless doing a view copy 
   * Allows copying a view to another CAS under a different name
   */
  private String mTgtCasViewName;
  private LowLevelCAS mLowLevelDestCas;
  final private Feature mDestSofaFeature;
  final private boolean lenient; //true: ignore feature structures and features that are not defined in the destination CAS

  /**
   * key is source FS, value is target FS 
   * Target not set for DocumentAnnotation or SofaFSs
   * Target not set if lenient specified and src type isn't in target
   */
  final private Map<FeatureStructure, FeatureStructure> mFsMap = new HashMap<FeatureStructure, FeatureStructure>();
  
  /**
   * feature structures whose slots need copying are put on this list, together with their source
   * List is operated as a stack, from the end, for efficiency
   */
  private ArrayList<FeatureStructure> fsToDo = new ArrayList<FeatureStructure>();

  /**
   * Creates a new CasCopier that can be used to copy FeatureStructures from one CAS to another.
   * Note that if you are merging data from multiple CASes, you must create a new CasCopier
   * for each source CAS.
   * 
   * Note: If the feature structure and/or feature is not defined in the type system of
   *       the destination CAS, the copy will fail (in other words, the lenient setting is false,
   *       by default).
   *       
   * @param aSrcCas
   *          the CAS to copy from.
   * @param aDestCas
   *          the CAS to copy into.
   */
  public CasCopier(CAS aSrcCas, CAS aDestCas) {
    this(aSrcCas, aDestCas, false);
  }

  /**
   * Creates a new CasCopier that can be used to copy FeatureStructures from one CAS to another.
   * Note that if you are merging data from multiple CASes, you must create a new CasCopier
   * for each source CAS. This version of the constructor supports a "lenient copy" option. When set,
   * the CAS copy function will ignore (not attempt to copy) FSs and features not defined in the type system
   * of the destination CAS, rather than throwing an exception.
   * 
   * @param aSrcCas
   *          the CAS to copy from.
   * @param aDestCas
   *          the CAS to copy into.
   * @param lenient
   *          ignore FSs and features not defined in the type system of the destination CAS
   */
  public CasCopier(CAS aSrcCas, CAS aDestCas, boolean lenient) {
    mOriginalSrcCasView = aSrcCas;
    mOriginalTgtCasView = aDestCas;
    
    mDestSofaFeature = aDestCas.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFA);    
    this.lenient = lenient;
  }
  
  /**
   * Does a complete deep copy of one CAS into another CAS.  The contents of each view
   * in the source CAS will be copied to the same-named view in the destination CAS.  If
   * the view does not already exist it will be created.  All FeatureStructures that are
   * indexed in a view in the source CAS will become indexed in the same-named view in the
   * destination CAS.
   * 
   * Note: If the feature structure and/or feature is not defined in the type system of
   *       the destination CAS, the copy will fail (in other words, the lenient setting is false,
   *       by default).
   *
   * @param aSrcCas
   *          the CAS to copy from
   * @param aDestCas
   *          the CAS to copy to
   * @param aCopySofa
   *          if true, the sofa data and mimeType of each view will be copied. If false they will not.
   */  
  public static void copyCas(CAS aSrcCas, CAS aDestCas, boolean aCopySofa) {
   copyCas(aSrcCas, aDestCas, aCopySofa, false);
  }

  /**
   * Does a complete deep copy of one CAS into another CAS.  The contents of each view
   * in the source CAS will be copied to the same-named view in the destination CAS.  If
   * the view does not already exist it will be created.  All FeatureStructures that are
   * indexed in a view in the source CAS will become indexed in the same-named view in the
   * destination CAS. This version of the method supports a "lenient copy" option. When set,
   * the CAS copy function will ignore (not attempt to copy) FSs and features not defined in the type system
   * of the destination CAS, rather than throwing an exception.
   *
   * @param aSrcCas
   *          the CAS to copy from
   * @param aDestCas
   *          the CAS to copy to; must be a completely different CAS than the source (that is, not an alternative "view" of the source)
   * @param aCopySofa
   *          if true, the sofa data and mimeType of each view will be copied. If false they will not.
   * @param lenient
   *          ignore FSs and features not defined in the type system of the destination CAS
   */
  public static void copyCas(CAS aSrcCas, CAS aDestCas, boolean aCopySofa, boolean lenient) {
    CasCopier copier = new CasCopier(aSrcCas, aDestCas, lenient);
    
    // oops, this misses the initial view if a sofa FS has not yet been created
//    Iterator<SofaFS> sofaIter = aSrcCas.getSofaIterator();
//    while (sofaIter.hasNext()) {
//      SofaFS sofa = sofaIter.next();
//      CAS view = aSrcCas.getView(sofa);
//      copier.copyCasView(view, aCopySofa);
//    }
    
    Iterator<CAS> viewIterator = aSrcCas.getViewIterator();
    while (viewIterator.hasNext()) {
      CAS view = viewIterator.next();
      copier.copyCasView(view, aCopySofa);     
    }
  }

  /**
   * Does a deep copy of the contents of one CAS View into another CAS's same-named-view
   * If the destination view already exists in the destination CAS,
   * then it will be the target of the copy.  Otherwise, a new view will be created with
   * that name and will become the target of the copy.  All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.
   * 
   * @param aSrcCasView the CAS to copy from.  This must be a view in the src Cas set by the constructor
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(CAS aSrcCasView, boolean aCopySofa) {
    copyCasView(aSrcCasView, getOrCreateView(mOriginalTgtCasView, aSrcCasView.getViewName()), aCopySofa);
  }
  
  /**
   * Does a deep copy of the contents of one CAS View into another CAS's same-named-view
   * If the destination view already exists in the destination CAS,
   * then it will be the target of the copy.  Otherwise, a new view will be created with
   * that name and will become the target of the copy.  All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   *
   * @param aSrcCasViewName the name of the view in the source CAS to copy from
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(String aSrcCasViewName, boolean aCopySofa) {
    copyCasView(getOrCreateView(mOriginalSrcCasView, aSrcCasViewName), aCopySofa);
  }
  
  /**
   * Does a deep copy of the contents of one CAS View into another CAS view,
   * with a possibly different name.
   * If the destination view already exists in the destination CAS,
   * then it will be the target of the copy.  Otherwise, a new view will be created with
   * that name and will become the target of the copy.  All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   * 
   * @param aSrcCasView The view in the source to copy from
   * @param aTgtCasViewName The name of the view in the destination CAS to copy into
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(CAS aSrcCasView, String aTgtCasViewName, boolean aCopySofa) {
    copyCasView(aSrcCasView, getOrCreateView(mOriginalTgtCasView, aTgtCasViewName), aCopySofa);
  }

  /**
   * Does a deep copy of the contents of one CAS View into another CAS view,
   * with a possibly different name.
   * All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   * 
   * @param aSrcCasViewName The name of the view in the Source CAS to copy from
   * @param aTgtCasView The view in the destination CAS to copy into
   * @param aCopySofa if true, the sofa data and mimeType will be copied. If false they will not.
   */
  public void copyCasView(String aSrcCasViewName, CAS aTgtCasView, boolean aCopySofa) {
    copyCasView(getOrCreateView(mOriginalSrcCasView, aSrcCasViewName), aTgtCasView, aCopySofa);
  }

  /**
   * Does a deep copy of the contents of one CAS View into another CAS view,
   * with a possibly different name.
   * 
   * The CASes must be different (that is, they cannot be 2 views of the same CAS).  
   * 
   * All FeatureStructures 
   * (except for those dropped because the target type system doesn't have the needed type) that are indexed 
   * in the source CAS view will become indexed in the target view.
   * Cross-view references may result in creating additional views in the destination CAS;
   * for these views, any Sofa data in the source is *not* copied.  Any views created because
   * of cross-view references will have the same view name as in the source.
   * 
   * @param aSrcCasView
   *          the CAS view to copy from. This must be a view of the srcCas set in the constructor
   * @param aTgtCasView 
   *          the CAS view to copy to. This must be a view of the tgtCas set in the constructor
   * @param aCopySofa
   *          if true, the sofa data and mimeType will be copied. If false they will not.  
   *          If true and the sofa data is already set in the target, will throw CASRuntimeException        
   */
  public void copyCasView(CAS aSrcCasView, CAS aTgtCasView, boolean aCopySofa) {
//    if (aSrcCasView == aTgtCasView) {
//      throw new UIMARuntimeException(UIMARuntimeException.ILLEGAL_CAS_COPY_TO_SAME_CAS_SAME_VIEW, null);
//    }
    
//    if (aSrcCasView == ((CASImpl)aSrcCasView).getBaseCAS() ||
//        aTgtCasView == ((CASImpl)aTgtCasView).getBaseCAS())
//      throw new UIMARuntimeException(UIMARuntimeException.UNSUPPORTED_CAS_COPY_TO_OR_FROM_BASE_CAS, null);
    
    if (!casViewsInSameCas(aSrcCasView, mOriginalSrcCasView)) {
      throw new UIMARuntimeException(UIMARuntimeException.VIEW_NOT_PART_OF_CAS, new Object[] {"Source"});
    }
    if (!casViewsInSameCas(aTgtCasView, mOriginalTgtCasView)) {
      throw new UIMARuntimeException(UIMARuntimeException.VIEW_NOT_PART_OF_CAS, new Object[] {"Destination"});
    }
    
    mSrcCasViewName = aSrcCasView.getViewName(); 
    mTgtCasViewName = aTgtCasView.getViewName();

    if (null == mSrcCasViewName || null == mTgtCasViewName ) {
      throw new UIMARuntimeException(UIMARuntimeException.UNSUPPORTED_CAS_COPY_TO_OR_FROM_BASE_CAS, null);
    }
        
    mLowLevelDestCas = aTgtCasView.getLowLevelCAS();
    
    // The top level sofa associated with this view is copied (or not)
    
    if (aCopySofa) {
      // can't copy the SofaFS - just copy the sofa data and mime type
      SofaFS sofa = aSrcCasView.getSofa();
      if (null != sofa) {
        // if the sofa doesn't exist in the target, these calls will create it
        //  (view can exist without Sofa, at least for the initial view)
        String sofaMime = sofa.getSofaMime();
        if (aSrcCasView.getDocumentText() != null) {
          aTgtCasView.setSofaDataString(aSrcCasView.getDocumentText(), sofaMime);
        } else if (aSrcCasView.getSofaDataURI() != null) {
          aTgtCasView.setSofaDataURI(aSrcCasView.getSofaDataURI(), sofaMime);
        } else if (aSrcCasView.getSofaDataArray() != null) {
          aTgtCasView.setSofaDataArray(copyFs2(aSrcCasView.getSofaDataArray()), sofaMime);
        }
      }
    }

    // now copy indexed FS, but keep track so we don't index anything more than once
    //   Note: mFsMap might be used for this, but it doesn't index several kinds of FSs
    //         see the javadoc for this field for details
    // NOTE: FeatureStructure hashcode / equals use the int "address" of the FS in the heap.
    
    Set<FeatureStructure> indexedFs = new HashSet<FeatureStructure>();
    
    // The indexFs set starts out "cleared", but 
    // we don't clear the cas copier instance map "mFsMap" here, in order to skip actually copying the
    //   FSs when doing a full CAS copy with multiple views - the 2nd and subsequent
    //   views don't copy, but they do index.
    
    Iterator<FSIndex<FeatureStructure>> indexes = aSrcCasView.getIndexRepository().getIndexes();
    while (indexes.hasNext()) {
      FSIndex<FeatureStructure> index = indexes.next();
      Iterator<FeatureStructure> iter = index.iterator();
      while (iter.hasNext()) {
        FeatureStructure fs = iter.next();
        if (!indexedFs.contains(fs)) {
          FeatureStructure copyOfFs = copyFs2(fs);
          // If the lenient option is used, it's possible that no FS was
          // created (e.g., FS is not defined in the target CAS. So ignore
          // this FS in the source CAS and move on to the next FS.
          if (lenient && copyOfFs == null) {
            continue; // Move to the next FS in the source CAS
          }
          // otherwise, won't be null (error thrown instead)

          // check for annotations with null Sofa reference - this can happen
          // if the annotations were created with the Low Level CAS API. If the
          // Sofa reference isn't set, attempting to add the FS to the indexes
          // will fail.
          if (fs instanceof AnnotationBaseFS) {
            FeatureStructure sofa = ((AnnotationBaseFS) copyOfFs).getFeatureValue(mDestSofaFeature);
            if (sofa == null) {
              copyOfFs.setFeatureValue(mDestSofaFeature, aTgtCasView.getSofa());
            }
          }
          // also don't index the DocumentAnnotation (it's indexed by default)
          if (!isDocumentAnnotation(copyOfFs)) {
            aTgtCasView.addFsToIndexes(copyOfFs);
          }
          indexedFs.add(fs);
        }
      }
    }
  }

  /**
   * For long lists, and other structures, the straight-forward impl with recursion can
   * nest too deep, causing a Java failure - out of stack space.
   * 
   * This is a non-recursive impl, making use of an aux object: featureStructuresWithSlotsToSet to
   * hold copied FSs whose slots need to be scanned and set with values.
   * 
   * The main loop dequeues one element, and copies the features.
   * 
   * The copying of a FS copies the FS without setting the slots; instead it queues the
   * copied FS together with its source instance on featureStructuresWithSlotsToSet 
   * for later processing.
   * 
   */
  
  /**
   * Copy 1 feature structure to a new Cas View.  No indexing of the new FS is done.
   * If the FS has been copied previously (using this CasCopier instance) the 
   * same identical copy will be returned rather than making another copy.
   * 
   * @param aFS the Feature Structure to copy
   * @return a deep copy of the Feature Structure - any referred to FSs will also be copied.
   */
  
  public FeatureStructure copyFs(FeatureStructure aFS) {
    // note these variables are null if copyFs is called after
    //   creating an instance of this class
    if (null == mSrcCasViewName) {
      mSrcCasViewName = mOriginalSrcCasView.getViewName();  // may set it to null, if Cas is base view
    }
    
    if (null == mTgtCasViewName) {
      mTgtCasViewName = mOriginalTgtCasView.getViewName(); // may set it to null, if Cas is base view
    }
    
    if (null == mLowLevelDestCas) {
      mLowLevelDestCas = mOriginalTgtCasView.getLowLevelCAS();
    }
    
    return copyFs2(aFS);
  }
  
  private FeatureStructure copyFs2(FeatureStructure aFS) {
    
    FeatureStructure copy = copyFsInner(aFS);  // doesn't copy the slot values, but enqueues them
    while (!fsToDo.isEmpty()) {
      FeatureStructure copyToFillSlots = fsToDo.remove(fsToDo.size()-1);
      FeatureStructure srcToFillSlots = fsToDo.remove(fsToDo.size()-1);
      copyFeatures(srcToFillSlots, copyToFillSlots);   
    }
    return copy;
  }

  /**
   * Copies an FS from the source CAS to the destination CAS. Also copies any referenced FS, except
   * that previously copied FS will not be copied again.
   * 
   * @param aFS
   *          the FS to copy. Must be contained within the source CAS.
   * @return the copy of <code>aFS</code> in the target CAS.
   */
  private FeatureStructure copyFsInner(FeatureStructure aFS) {
    // FS must be in the source CAS
    assert (casViewsInSameCas(aFS.getCAS(), mOriginalSrcCasView));

    // check if we already copied this FS
    FeatureStructure copy = (FeatureStructure) mFsMap.get(aFS);
    if (copy != null)
      return copy;

    // get the type of the FS
    Type srcType = aFS.getType();

    // Certain types need to be handled specially

    // Sofa - cannot be created by normal methods. Instead, we return the Sofa with the
    // same Sofa ID in the target CAS. If it does not exist it will be created.
    if (aFS instanceof SofaFS) {
      String destSofaId = getDestSofaId(((SofaFS) aFS).getSofaID());
      return getOrCreateView(mOriginalTgtCasView, destSofaId).getSofa();
    }

    // DocumentAnnotation - instead of creating a new instance, reuse the automatically created
    // instance in the destination view.
    if (isDocumentAnnotation(aFS)) {
      String destViewName = getDestSofaId(((AnnotationFS) aFS).getView().getViewName());

      // the DocumentAnnotation could be indexed in a different view than the one being copied
      // Note: The view might not exist in the target
      //   but this is unlikely.  To have this case this would require
      //   indexing some other feature structure in this view, which, in turn,
      //   has a reference to the DocumentAnnotation FS belonging to another view
      CAS destView = getOrCreateView(mOriginalTgtCasView, destViewName);
      FeatureStructure destDocAnnot = destView.getDocumentAnnotation();
      if (destDocAnnot != null) {  // Note: is always non-null, getDocumentAnnotation creates if not exist
        copyFeatures(aFS, destDocAnnot);
      }
      return destDocAnnot;
    }

    // Arrays - need to be created a populated differently than "normal" FS
    if (aFS.getType().isArray()) {
      copy = copyArray(aFS);
      mFsMap.put(aFS, copy);
      return copy;
    }

    // create a new FS of the same type in the target CAS
    TypeSystem destTs = mOriginalTgtCasView.getTypeSystem();
    Type destType = (destTs == mOriginalSrcCasView.getTypeSystem()) ? 
        srcType : 
        destTs.getType(srcType.getName());
    if (destType == null) {
      // If in lenient mode, do not act on this FS. Instead just
      // return (null) to the caller and let the caller deal with this case.
      if (lenient) {
        return null; // No FS to create
      } else {
        throw new UIMARuntimeException(UIMARuntimeException.TYPE_NOT_FOUND_DURING_CAS_COPY,
            new Object[] { srcType.getName() });
      }
    }
    // We need to use the LowLevel CAS interface to create the FS, because the usual
    // CAS.createFS() call doesn't allow us to create subtypes of AnnotationBase from
    // a base CAS. In any case we don't need the Sofa reference to be automatically
    // set because we'll set it manually when in the copyFeatures method.
    
    int typeCode = mLowLevelDestCas.ll_getTypeSystem().ll_getCodeForType(destType);
    int destFsAddr = mLowLevelDestCas.ll_createFS(typeCode);
    FeatureStructure destFs = mLowLevelDestCas.ll_getFSForRef(destFsAddr);

    // add to map so we don't try to copy this more than once
    mFsMap.put(aFS, destFs);

    fsToDo.add(aFS); // order important
    fsToDo.add(destFs);
    return destFs;
  }
  
  private String getDestSofaId(String id) {
    return (null != mSrcCasViewName && mSrcCasViewName.equals(id)) ? mTgtCasViewName : id;
  }
  
  /**
   * Copy feature values from one FS to another. For reference-valued features, this does a deep
   * copy.
   * 
   * @param aSrcFS
   *          FeatureStructure to copy from
   * @param aDestFS
   *          FeatureStructure to copy to
   */
  private void copyFeatures(FeatureStructure aSrcFS, FeatureStructure aDestFS) {
    // set feature values
    Type srcType = aSrcFS.getType();
    Type destType = aDestFS.getType();
    for (Feature srcFeat : srcType.getFeatures()) {
      Feature destFeat;
      if (destType == aSrcFS.getType()) {
        // sharing same type system, so destFeat == srcFeat
        destFeat = srcFeat;
      } else {
        // not sharing same type system, so do a name loop up in destination type system
        destFeat = destType.getFeatureByBaseName(srcFeat.getShortName());
        if (destFeat == null) {
          // If in lenient mode, ignore this feature and move on to the next
          // feature in this FS (if one exists)
          if (lenient) {
            continue; // Ignore this feature in the source CAS since it doesn't exist in
                      // in the target CAS.
          } else {
            throw new UIMARuntimeException(UIMARuntimeException.FEATURE_NOT_FOUND_DURING_CAS_COPY,
                new Object[] { srcFeat.getName() });
          }
        }
      }

      // copy primitive values using their string representation
      // TODO: could be optimized but this code would be very messy if we have to
      // enumerate all possible primitive types. Maybe LowLevel CAS API could help?
      String srcRangeName = srcFeat.getRange().getName();
      if (srcRangeName.equals(CAS.TYPE_NAME_STRING)) {
        aDestFS.setStringValue(destFeat, aSrcFS.getStringValue(srcFeat));
      } else if (srcRangeName.equals(CAS.TYPE_NAME_INTEGER)) {
        aDestFS.setIntValue(destFeat, aSrcFS.getIntValue(srcFeat));
      } else if (srcFeat.getRange().isPrimitive()) {
        aDestFS.setFeatureValueFromString(destFeat, aSrcFS.getFeatureValueAsString(srcFeat));
      } else {
        // recursive copy no longer done recursively, to avoid blowing the stack
        FeatureStructure refFS = aSrcFS.getFeatureValue(srcFeat);
        if (refFS != null) {
          FeatureStructure copyRefFs = copyFsInner(refFS);
          aDestFS.setFeatureValue(destFeat, copyRefFs);
        }
      }
    }
  }

  /**
   * Note: if lenient is in effect, this method will return false for
   * FSs which are not copied because the target doesn't have that type.
   * It also returns false for sofa FSs and the documentAnnotation FS.
   * @param aFS a feature structure
   * @return true if the given FS has already been copied using this CasCopier.
   */
  public boolean alreadyCopied(FeatureStructure aFS) {
    return mFsMap.containsKey(aFS);
  }

  /**
   * @param arrayFS
   * @return a copy of the array
   */
  private FeatureStructure copyArray(FeatureStructure aSrcFs) {
    // TODO: there should be a way to do this without enumerating all the array types!
    if (aSrcFs instanceof StringArrayFS) {
      StringArrayFS arrayFs = (StringArrayFS) aSrcFs;
      int len = arrayFs.size();
      StringArrayFS destFS = mOriginalTgtCasView.createStringArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof IntArrayFS) {
      IntArrayFS arrayFs = (IntArrayFS) aSrcFs;
      int len = arrayFs.size();
      IntArrayFS destFS = mOriginalTgtCasView.createIntArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof ByteArrayFS) {
      ByteArrayFS arrayFs = (ByteArrayFS) aSrcFs;
      int len = arrayFs.size();
      ByteArrayFS destFS = mOriginalTgtCasView.createByteArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof ShortArrayFS) {
      ShortArrayFS arrayFs = (ShortArrayFS) aSrcFs;
      int len = arrayFs.size();
      ShortArrayFS destFS = mOriginalTgtCasView.createShortArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof LongArrayFS) {
      LongArrayFS arrayFs = (LongArrayFS) aSrcFs;
      int len = arrayFs.size();
      LongArrayFS destFS = mOriginalTgtCasView.createLongArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof FloatArrayFS) {
      FloatArrayFS arrayFs = (FloatArrayFS) aSrcFs;
      int len = arrayFs.size();
      FloatArrayFS destFS = mOriginalTgtCasView.createFloatArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof DoubleArrayFS) {
      DoubleArrayFS arrayFs = (DoubleArrayFS) aSrcFs;
      int len = arrayFs.size();
      DoubleArrayFS destFS = mOriginalTgtCasView.createDoubleArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof BooleanArrayFS) {
      BooleanArrayFS arrayFs = (BooleanArrayFS) aSrcFs;
      int len = arrayFs.size();
      BooleanArrayFS destFS = mOriginalTgtCasView.createBooleanArrayFS(len);
      for (int i = 0; i < len; i++) {
        destFS.set(i, arrayFs.get(i));
      }
      return destFS;
    }
    if (aSrcFs instanceof ArrayFS) {
      ArrayFS arrayFs = (ArrayFS) aSrcFs;
      int len = arrayFs.size();
      ArrayFS destFS = mOriginalTgtCasView.createArrayFS(len);
      for (int i = 0; i < len; i++) {
        FeatureStructure srcElem = arrayFs.get(i);
        if (srcElem != null) {
          FeatureStructure copyElem = copyFsInner(arrayFs.get(i));
          destFS.set(i, copyElem);
        }
      }
      return destFS;
    }
    assert false; // the set of array types should be exhaustive, so we should never get here
    return null;
  }
  
  /**
   * Gets the named view; if the view doesn't exist it will be created.
   */
  private static CAS getOrCreateView(CAS aCas, String aViewName) {
    //TODO: there should be some way to do this without the try...catch
    try { // throws if view doesn't exist
      return aCas.getView(aViewName); 
    }
    catch(CASRuntimeException e) {
      //create the view
      return aCas.createView(aViewName); 
    }
  }  
  
  /**
   * Determines whether the given FS is the DocumentAnnotation for its view.  
   * This is more than just a type check; we actually check if it is the one "special"
   * DocumentAnnotation that CAS.getDocumentAnnotation() would return.
   */
  private static boolean isDocumentAnnotation(FeatureStructure aFS) {
    return (aFS instanceof AnnotationFS) &&
      aFS.equals(((AnnotationFS)aFS).getView().getDocumentAnnotation());
  }
  
  /**
   * Important: only use CAS apis, not CASImpl APIs (https://issues.apache.org/jira/browse/UIMA-3112)
   * @param c1
   * @param c2
   * @return true if both views are in the same CAS (e.g., they have the same base CAS)
   */
  private boolean casViewsInSameCas(CAS c1, CAS c2) {
    if (null == c1 || null == c2) {
      return false;
    }
    
    // if the cas's are equal, then both views are in the same CAS, of course
    // This test isn't logically needed, but it allows this case to pass if 
    // incorrect wrappers are supplied, where the wrapper fails to 
    // notice that a getView returns the same original CAS, (which the wrapper would
    // need to replace with the wrapped version)
    if (c1.equals(c2)) {
      return true;
    }
    
    String v1 = c1.getViewName();
    String v2 = c2.getViewName();
    
    if (v1 == null) {
      // means it's not the initial view, and 
      // this view has no sofa FS (yet)
      // -- likely is a base CAS
      if (v2 == null) {
        // two base CASes case
        return c1.equals(c2);  // // defaults to object ==, but wrappers might implement something else
      }
      // we don't have a way to get to the base cas directly in the CAS Api
      try { // throws if view doesn't exist
        return c1.getView(v2).equals(c2);  // defaults to object ==, but wrappers might implement something else
      } catch (CASRuntimeException e) {
        return false;
      }
    }
    try { // throws if view doesn't exist
      return c2.getView(v1).equals(c1); // defaults to object ==, but wrappers might implement something else
    } catch (CASRuntimeException e) {
      return false;
    }
  }
}

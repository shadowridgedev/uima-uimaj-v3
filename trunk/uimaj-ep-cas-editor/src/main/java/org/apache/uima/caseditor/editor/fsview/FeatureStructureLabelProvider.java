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

package org.apache.uima.caseditor.editor.fsview;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.FeatureValue;
import org.apache.uima.jcas.cas.TOP;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;


/**
 * Provide the labels for the given {@link FeatureStructure}s.
 */
public final class FeatureStructureLabelProvider implements ILabelProvider {
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  @Override
  public String getText(Object element) {
    if (element instanceof FeatureValue) {
      FeatureValue featureValue = (FeatureValue) element;
      Object value = featureValue.getValue();

      if (value == null) {
        return featureValue.getFeature().getShortName() + ": null";
      }

      if (featureValue.getFeature().getRange().isPrimitive()) {
        return featureValue.getFeature().getShortName() + " : " + value.toString();
      }

      return featureValue.getFeature().getShortName();
    }
    else if (element instanceof IAdaptable) {

      FeatureStructure structure = null;

      if (((IAdaptable) element).getAdapter(AnnotationFS.class) != null) {
        structure = (AnnotationFS) ((IAdaptable) element)
                .getAdapter(AnnotationFS.class);
      }

      if (structure == null) {
        structure = (FeatureStructure) ((IAdaptable) element).getAdapter(FeatureStructure.class);
      }

      return structure.getType().getShortName() + " (id=" +
      		((TOP) structure)._id() + ")";
    }
    else {
      assert false : "Unexpected element!";

      return element.toString();
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
   */
  @Override
  public Image getImage(Object element) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
   */
  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
   */
  @Override
  public void addListener(ILabelProviderListener listener) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
   */
  @Override
  public void removeListener(ILabelProviderListener listener) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
  }
}
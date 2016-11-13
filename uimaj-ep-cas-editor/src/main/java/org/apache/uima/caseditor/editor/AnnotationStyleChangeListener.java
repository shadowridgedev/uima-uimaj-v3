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

package org.apache.uima.caseditor.editor;

import java.util.Collections;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

// TODO: Auto-generated Javadoc
/**
 * Annotation Style Change Listener base class which converts annotation style change events from
 * a Preference Store to AnnotationStyle object change events.
 * <p>
 * An implementing class needs to override the annotationStylesChanged method.
 */
public abstract class AnnotationStyleChangeListener
    implements IPropertyChangeListener, IAnnotationStyleListener {

  /* (non-Javadoc)
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().endsWith(".style")) {
      // extract type name ...
      String typeName = event.getProperty().substring(0, event.getProperty().lastIndexOf(".style"));
      AnnotationStyle style = AnnotationStyle.getAnnotationStyleFromStore(
              (IPreferenceStore) event.getSource(), typeName);
      annotationStylesChanged(Collections.singleton(style));
    }
  }
}

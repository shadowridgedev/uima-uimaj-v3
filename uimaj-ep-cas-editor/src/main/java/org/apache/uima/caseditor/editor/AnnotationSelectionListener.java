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

import java.util.Collection;

import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;


/**
 * TODO: add javadoc here.

 */
public abstract class AnnotationSelectionListener implements ISelectionListener {

  /* (non-Javadoc)
   * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
   */
  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
  }

  /**
   * Selected annotation.
   *
   * @param annotations the annotations
   */
  protected abstract void selectedAnnotation(Collection<AnnotationFS> annotations);

  /**
   * Indicates that the selection did not contain any annotations.
   */
  protected abstract void emptySelection();
}

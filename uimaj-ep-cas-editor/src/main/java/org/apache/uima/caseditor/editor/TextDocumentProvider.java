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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;


/**
 * The Class TextDocumentProvider.
 */
class TextDocumentProvider extends AbstractDocumentProvider {
  
  /**
   * The Class CasElementInfo.
   */
  private class CasElementInfo extends AbstractDocumentProvider.ElementInfo {
    
    /** The cas info. */
    private CasDocumentProvider.ElementInfo casInfo;
    
    /**
     * Instantiates a new cas element info.
     *
     * @param document the document
     * @param model the model
     */
    public CasElementInfo(IDocument document, IAnnotationModel model) {
      super(document, model);
    }
  }
  
  /** The document provider. */
  private final CasDocumentProvider documentProvider;
  
  /**
   * Instantiates a new text document provider.
   *
   * @param documentProvider the document provider
   */
  public TextDocumentProvider(CasDocumentProvider documentProvider) {
    this.documentProvider = documentProvider;
    
    this.documentProvider.addElementStateListener(new IElementStateListener() {
      
      @Override
      public void elementMoved(Object originalElement, Object movedElement) {
        fireElementMoved(originalElement, movedElement);
      }
      
      @Override
      public void elementDirtyStateChanged(Object element, boolean isDirty) {
        fireElementDirtyStateChanged(element, isDirty);
      }
      
      @Override
      public void elementDeleted(Object element) {
        fireElementDeleted(element);
      }
      
      @Override
      public void elementContentReplaced(Object element) {
        fireElementContentReplaced(element);
      }
      
      @Override
      public void elementContentAboutToBeReplaced(Object element) {
        fireElementContentAboutToBeReplaced(element);
      }
    });
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createAnnotationModel(java.lang.Object)
   */
  @Override
  protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
    return new org.eclipse.jface.text.source.AnnotationModel();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createDocument(java.lang.Object)
   */
  @Override
  protected IDocument createDocument(Object element) throws CoreException {
    ICasDocument casDocument =  documentProvider.createDocument(element);
    
    if (casDocument != null) {
      AnnotationDocument document = new AnnotationDocument();
      document.setDocument(casDocument);
      return document;
    }
    else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#doSaveDocument(org.eclipse.core.runtime.IProgressMonitor, java.lang.Object, org.eclipse.jface.text.IDocument, boolean)
   */
  @Override
  protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document,
          boolean overwrite) throws CoreException {
    
    if (document instanceof AnnotationDocument) {
      AnnotationDocument annotationDocument = (AnnotationDocument) document;
      documentProvider.doSaveDocument(monitor, element, annotationDocument.getDocument(), overwrite);
    }
    // TODO:
    // else throw exception ->
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getOperationRunner(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
    return null;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createElementInfo(java.lang.Object)
   */
  @Override
  protected ElementInfo createElementInfo(Object element) throws CoreException {
    
    ElementInfo elementInfo = super.createElementInfo(element);
    CasElementInfo casElementInfo = new CasElementInfo(elementInfo.fDocument, elementInfo.fModel);
    casElementInfo.casInfo = documentProvider.createElementInfo(element);
    
    return casElementInfo;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#disposeElementInfo(java.lang.Object, org.eclipse.ui.texteditor.AbstractDocumentProvider.ElementInfo)
   */
  @Override
  protected void disposeElementInfo(Object element, ElementInfo info) {
    
    super.disposeElementInfo(element, info);
    
    CasElementInfo casElementInfo = (CasElementInfo) info;
    documentProvider.disposeElementInfo(element, casElementInfo.casInfo);
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getStatus(java.lang.Object)
   */
  @Override
  public IStatus getStatus(Object element) {
    IStatus status = documentProvider.getStatus(element);

    if (status == null) {
      status = super.getStatus(element);
    }

    return status;
  }
}

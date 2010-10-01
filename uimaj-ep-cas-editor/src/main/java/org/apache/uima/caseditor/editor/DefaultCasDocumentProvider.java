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

import org.apache.uima.cas.Type;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.DocumentElement;
import org.apache.uima.caseditor.core.model.INlpElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.part.FileEditorInput;

public class DefaultCasDocumentProvider extends
        org.apache.uima.caseditor.editor.CasDocumentProvider {

  @Override
  protected IDocument createDocument(Object element) throws CoreException {
    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      INlpElement nlpElement = CasEditorPlugin.getNlpModel().findMember(file);

      if (nlpElement instanceof DocumentElement) {

        try {
          org.apache.uima.caseditor.editor.ICasDocument workingCopy =
                  ((DocumentElement) nlpElement).getDocument(true);

          AnnotationDocument document = new AnnotationDocument();

          document.setDocument(workingCopy);

          elementErrorStatus.remove(element);

          return document;
        } catch (CoreException e) {
          elementErrorStatus.put(element, new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK,
                  "There is a problem with the document: " + e.getMessage(), e));
        }
      } else {
        IStatus status;

        if (nlpElement == null) {
          status = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK,
                  "Document not in a corpus folder!", null);
        } else {
          status = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, "Not a cas document!",
                  null);
        }

        elementErrorStatus.put(element, status);
      }
    }

    return null;
  }

  @Override
  protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document,
          boolean overwrite) throws CoreException {

    fireElementStateChanging(element);

    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      INlpElement nlpElement =
              org.apache.uima.caseditor.CasEditorPlugin.getNlpModel().findMember(file);

      if (nlpElement instanceof DocumentElement) {
        DocumentElement documentElement = (DocumentElement) nlpElement;

        try {
          documentElement.saveDocument();
        } catch (CoreException e) {
          fireElementStateChangeFailed(element);
          throw e;
        }
      } else {
        fireElementStateChangeFailed(element);
        return;
      }
    }

    fireElementDirtyStateChanged(element, false);
  }

  private INlpElement getNlpElement(Object element) {
    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      return CasEditorPlugin.getNlpModel().findMember(file);
    }

    return null;
  }

  @Override
  public AnnotationStyle getAnnotationStyle(Object element, Type type) {
    
    if (type == null)
    	throw new IllegalArgumentException("type parameter must not be null!");
    
    INlpElement nlpElement = getNlpElement(element);

    return nlpElement.getNlpProject().getDotCorpus().getAnnotation(type);
  }

  @Override
  public void setAnnotationStyle(Object element, AnnotationStyle style) {
    INlpElement nlpElement = getNlpElement(element);

    nlpElement.getNlpProject().getDotCorpus().setStyle(style);
  }
  
  @Override
  protected Collection<String> getShownTypes(Object element) {
    INlpElement nlpElement = getNlpElement(element);

    return nlpElement.getNlpProject().getDotCorpus().getShownTypes();
  }
  
  @Override
  protected void addShownType(Object element, Type type) {
    INlpElement nlpElement = getNlpElement(element);
    
    nlpElement.getNlpProject().getDotCorpus().addShownType(type.getName());
    
    try {
      nlpElement.getNlpProject().getDotCorpus().serialize();
    } catch (CoreException e) {
      CasEditorPlugin.log(e);
    }
  }
  
  @Override
  protected void removeShownType(Object element, Type type) {
    INlpElement nlpElement = getNlpElement(element);

    nlpElement.getNlpProject().getDotCorpus().removeShownType(type.getName());
    
    try {
      nlpElement.getNlpProject().getDotCorpus().serialize();
    } catch (CoreException e) {
      CasEditorPlugin.log(e);
    }
  }
  
  @Override
  protected EditorAnnotationStatus getEditorAnnotationStatus(Object element) {
    INlpElement nlpElement = getNlpElement(element);

    return nlpElement.getNlpProject().getEditorAnnotationStatus();
  }

  @Override
  protected void setEditorAnnotationStatus(Object element,
          EditorAnnotationStatus editorAnnotationStatus) {
    INlpElement nlpElement = getNlpElement(element);

    nlpElement.getNlpProject().setEditorAnnotationStatus(editorAnnotationStatus);
  }
}

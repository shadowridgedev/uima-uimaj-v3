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

import org.apache.uima.cas.CAS;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

// TODO: Auto-generated Javadoc
/**
 * Base view for views which show information about the {@link CAS} opened
 * in the editor.
 */
public abstract class AnnotationEditorView extends PageBookView {

  /** The editor not available message. */
  private final String editorNotAvailableMessage;

  /**
   * Instantiates a new annotation editor view.
   *
   * @param editorNotAvailableMessage the editor not available message
   */
  public AnnotationEditorView(String editorNotAvailableMessage) {
    this.editorNotAvailableMessage = editorNotAvailableMessage;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.PageBookView#createDefaultPage(org.eclipse.ui.part.PageBook)
   */
  @Override
  protected IPage createDefaultPage(PageBook book) {
    MessagePage page = new MessagePage();
    initPage(page);
    page.createControl(book);
    page.setMessage(editorNotAvailableMessage);
    return page;
  }

  /**
   * Do create page.
   *
   * @param editor the editor
   * @return the page rec
   */
  protected abstract PageRec doCreatePage(ICasEditor editor);

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.PageBookView#doCreatePage(org.eclipse.ui.IWorkbenchPart)
   */
  @Override
  protected final PageRec doCreatePage(IWorkbenchPart part) {

    if (part instanceof AnnotationEditor) {
      ICasEditor editor = (ICasEditor) part;

      return doCreatePage(editor);
    }

    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.PageBookView#getBootstrapPart()
   */
  @Override
  protected IWorkbenchPart getBootstrapPart() {
    return getSite().getPage().getActiveEditor();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.PageBookView#doDestroyPage(org.eclipse.ui.IWorkbenchPart, org.eclipse.ui.part.PageBookView.PageRec)
   */
  @Override
  protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
    pageRecord.page.dispose();

    pageRecord.dispose();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.PageBookView#isImportant(org.eclipse.ui.IWorkbenchPart)
   */
  @Override
  protected boolean isImportant(IWorkbenchPart part) {
    // only interested in annotation editors
    return part instanceof AnnotationEditor;
  }

  /**
   * Look at {@link IPartListener#partBroughtToTop(IWorkbenchPart)}.
   *
   * @param part the part
   */
  @Override
  public void partBroughtToTop(IWorkbenchPart part) {
    partActivated(part);
  }
}

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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;


/**
 * The Class CasEditorViewPage.
 */
public class CasEditorViewPage extends Page implements ISelectionProvider {

  /** The selection changed listeners. */
  private ListenerList selectionChangedListeners = new ListenerList();
  
  /** The not available message. */
  private final String notAvailableMessage;
  
  /** The book. */
  protected PageBook book;
  
  /** The cas view page. */
  protected IPageBookViewPage casViewPage;

  /** The sub action bar. */
  private SubActionBars subActionBar;
  
  /** The message text. */
  private Text messageText;
  
  /**
   * Instantiates a new cas editor view page.
   *
   * @param notAvailableMessage the not available message
   */
  protected CasEditorViewPage(String notAvailableMessage) {
    this.notAvailableMessage = notAvailableMessage;
  }
  
  /**
   * Refresh action handlers.
   */
  @SuppressWarnings("rawtypes")
  private void refreshActionHandlers() {

    IActionBars actionBars = getSite().getActionBars();
    actionBars.clearGlobalActionHandlers();

    Map newActionHandlers = subActionBar
        .getGlobalActionHandlers();
    if (newActionHandlers != null) {
      Set keys = newActionHandlers.entrySet();
      Iterator iter = keys.iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        actionBars.setGlobalActionHandler((String) entry.getKey(),
            (IAction) entry.getValue());
      }
    }
  }

  // These are called from the outside, even if the page is not active ...
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  // this leads to the processing of events which should not be processed!
  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.add(listener);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
   */
  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.remove(listener);
  }
  
  /**
   * Selection changed.
   *
   * @param event the event
   */
  public void selectionChanged(final SelectionChangedEvent event) {
    
    for (Object listener : selectionChangedListeners.getListeners()) {
      
      final ISelectionChangedListener selectionChangedListener = 
              (ISelectionChangedListener) listener;
      
      SafeRunner.run(new SafeRunnable() {
        @Override
        public void run() {
          selectionChangedListener.selectionChanged(event);
        }
      });
    }
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
   */
  @Override
  public ISelection getSelection() {
    if (casViewPage != null && casViewPage.getSite().getSelectionProvider() != null) {
      return casViewPage.getSite().getSelectionProvider().getSelection();
    }
    else {
      return StructuredSelection.EMPTY;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
   */
  @Override
  public void setSelection(ISelection selection) {
    if (casViewPage != null && casViewPage.getSite().getSelectionProvider() != null) {
      casViewPage.getSite().getSelectionProvider().setSelection(selection);
    }
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.part.Page#createControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createControl(Composite parent) {
    book = new PageBook(parent, SWT.NONE);
    
    messageText = new Text(book, SWT.WRAP | SWT.READ_ONLY);
    messageText.setBackground(parent.getShell().getBackground());
    messageText.setText(notAvailableMessage);
    
    getSite().setSelectionProvider(this);
    
    // Page might be set before the page is initialized
    initializeAndShowPage(casViewPage);
  }
  
  /**
   * Creates and shows the page, if page is null
   * the not available message will be shown.
   *
   * @param page the page
   */
  protected void initializeAndShowPage(IPageBookViewPage page) {
    if (book != null) {
      if (page != null) {
        page.createControl(book);
        casViewPage = page;
        
        // Note: If page is in background event listening must be disabled!
        ISelectionProvider selectionProvider = page.getSite().getSelectionProvider();
        selectionProvider.addSelectionChangedListener(new ISelectionChangedListener() {
          
          @Override
          public void selectionChanged(SelectionChangedEvent event) {
            CasEditorViewPage.this.selectionChanged(event);
          }
        });
        
        subActionBar = (SubActionBars) casViewPage.getSite().getActionBars();
        
        casViewPage.setActionBars(subActionBar);

        subActionBar.activate();
        subActionBar.updateActionBars();

        refreshActionHandlers();
        
        book.showPage(page.getControl());
      }
      else {
        book.showPage(messageText);
        getSite().getActionBars().updateActionBars();
      }
    }
  }
  
  /**
   * Sets the CAS view page.
   *
   * @param page the new CAS view page
   */
  public void setCASViewPage(IPageBookViewPage page) {
    
    if (book != null && casViewPage != null) {
      casViewPage.dispose();
      subActionBar.dispose();
    }
    
    casViewPage = page;
    
    initializeAndShowPage(page);
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.part.Page#getControl()
   */
  @Override
  public Control getControl() {
    return book;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.Page#setFocus()
   */
  @Override
  public void setFocus() {
    book.setFocus();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.part.Page#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();

    if (casViewPage != null) {
      casViewPage.dispose();
      subActionBar.dispose();
    }
  }
}

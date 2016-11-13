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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;

// TODO: Auto-generated Javadoc
/**
 * The Class SearchMonitoringThread.
 */
public class SearchMonitoringThread extends Thread {
  
  /** The m search thread. */
  private SearchThread m_searchThread;

  /** The m dialog. */
  FindComponentDialog m_dialog;

  /**
   * Instantiates a new search monitoring thread.
   *
   * @param dialog the dialog
   * @param searchThread the search thread
   */
  public SearchMonitoringThread(FindComponentDialog dialog, SearchThread searchThread) {
    m_dialog = dialog;
    m_searchThread = searchThread;
  }

  /* (non-Javadoc)
   * @see java.lang.Thread#run()
   */
  @Override
  public void run() {
    while (true) {
      if (m_searchThread.isDone()) {
        if (!m_dialog.getStatusLabel1().isDisposed()) {
          Display display = m_dialog.getStatusLabel1().getDisplay();
          display.syncExec(new Runnable() {
            @Override
            public void run() {
              m_dialog.buttonPressed(IDialogConstants.CANCEL_ID);
            }
          });
        }
        return;
      }

      try {
        Thread.sleep(500);
      } catch (Exception ex) {
      }
    }
  }
}

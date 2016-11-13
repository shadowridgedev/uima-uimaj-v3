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

package org.apache.uima.tools.docanalyzer;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// TODO: Auto-generated Javadoc
/**
 * * This class tells the Mediator to check the length of the 3 text fields and adjust whether the 3
 * buttons are enabeld or not.
 * 
// * @see TfDocumentEvent
 */
public class TfDocumentListener implements DocumentListener {
  
  /** The med. */
  private PrefsMediator med;

  /**
   * Instantiates a new tf document listener.
   *
   * @param med the med
   */
  public TfDocumentListener(PrefsMediator med) {
    this.med = med;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
   */
  @Override
  public void changedUpdate(DocumentEvent e) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
   */
  @Override
  public void insertUpdate(DocumentEvent e) {
    med.fieldFocusLost();
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
   */
  @Override
  public void removeUpdate(DocumentEvent e) {
    med.fieldFocusLost();
  }

}

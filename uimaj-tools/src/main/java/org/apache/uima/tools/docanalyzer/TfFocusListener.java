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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;


/**
 * This class tells the Mediator to check the lenght of the 3 text fields and adjust whether the 3
 * buttons are enabeld or not.
 * 
// * @see TfFocusEvent
 */
public class TfFocusListener implements FocusListener {
  
  /** The med. */
  private PrefsMediator med;

  /**
   * Instantiates a new tf focus listener.
   *
   * @param med the med
   */
  public TfFocusListener(PrefsMediator med) {
    this.med = med;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
   */
  @Override
  public void focusGained(FocusEvent arg0) {

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
   */
  @Override
  public void focusLost(FocusEvent arg0) {
    // Tell the mediator the text may have changed.
    med.fieldFocusLost();
  }

}

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

package org.apache.uima.tools.cvd.control;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import org.apache.uima.tools.cvd.MainFrame;

// TODO: Auto-generated Javadoc
/**
 * The listener interface for receiving sofaSelection events.
 * The class that is interested in processing a sofaSelection
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addSofaSelectionListener</code> method. When
 * the sofaSelection event occurs, that object's appropriate
 * method is invoked.
 *
// * @see SofaSelectionEvent
 */
public class SofaSelectionListener implements ItemListener {

  /** The main. */
  private final MainFrame main;

  /**
   * Instantiates a new sofa selection listener.
   *
   * @param frame the frame
   */
  public SofaSelectionListener(MainFrame frame) {
    this.main = frame;
  }

  /* (non-Javadoc)
   * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
   */
  @Override
  public void itemStateChanged(ItemEvent e) {
    // a new sofa was selected. Switch to that view and update
    // display
    String sofaId = (String) e.getItem();
    this.main.setCas(this.main.getCas().getView(sofaId));
    String text = this.main.getCas().getDocumentText();
    if (text == null) {
      text = this.main.getCas().getSofaDataURI();
      if (text != null) {
        text = "SofaURI = " + text;
      } else {
        if (null != this.main.getCas().getSofaDataArray()) {
          text = "Sofa array with mime type = " + this.main.getCas().getSofa().getSofaMime();
        }
      }
    }
    String oldText = this.main.getTextArea().getText();
    if ((oldText == null) || (text == null) || !oldText.equals(text)) {
      this.main.setText(text);
    }
    if (text == null) {
      this.main.getTextArea().repaint();
    }
    this.main.updateIndexTree(true);
  }
}
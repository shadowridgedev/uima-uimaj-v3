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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.apache.uima.tools.cvd.MainFrame;

// TODO: Auto-generated Javadoc
/**
 * The Class CloseTypeSystemHandler.
 */
public class CloseTypeSystemHandler extends WindowAdapter implements WindowListener {

  /** The main. */
  private final MainFrame main;

  /**
   * Instantiates a new close type system handler.
   *
   * @param frame the frame
   */
  public CloseTypeSystemHandler(MainFrame frame) {
    this.main = frame;
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
   */
  @Override
  public void windowClosing(WindowEvent event) {
    JComponent tsContentPane = (JComponent) ((JFrame) event.getComponent()).getContentPane();
    final int x = tsContentPane.getWidth();
    final int y = tsContentPane.getHeight();
    this.main.getPreferences().setProperty(MainFrame.tsWindowSizePref + MainFrame.widthSuffix,
        Integer.toString(x));
    this.main.getPreferences().setProperty(MainFrame.tsWindowSizePref + MainFrame.heightSuffix,
        Integer.toString(y));
  }

}
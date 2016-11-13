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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.apache.uima.tools.cvd.MainFrame;

// TODO: Auto-generated Javadoc
/**
 * The Class ColorPrefsSaveHandler.
 */
public class ColorPrefsSaveHandler implements ActionListener {

  /** The main. */
  private final MainFrame main;

  /**
   * Instantiates a new color prefs save handler.
   *
   * @param frame the frame
   */
  public ColorPrefsSaveHandler(MainFrame frame) {
    this.main = frame;
  }

  /**
   * Action performed.
   *
   * @param event the event
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent event) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save color preferences");
    if (this.main.getColorSettingsDir() != null) {
      fileChooser.setCurrentDirectory(this.main.getColorSettingsDir());
    }
    int rc = fileChooser.showSaveDialog(this.main);
    if (rc == JFileChooser.APPROVE_OPTION) {
      File prefFile = fileChooser.getSelectedFile();
      this.main.setColorSettingsDir(prefFile.getParentFile());
      try {
        this.main.saveColorPreferences(prefFile);
      } catch (IOException e) {
        this.main.handleException(e);
      }
    }
  }

}
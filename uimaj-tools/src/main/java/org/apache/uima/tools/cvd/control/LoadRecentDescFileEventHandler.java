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

import org.apache.uima.tools.cvd.MainFrame;

// TODO: Auto-generated Javadoc
/**
 * The Class LoadRecentDescFileEventHandler.
 */
public class LoadRecentDescFileEventHandler implements ActionListener {

  /** The main. */
  private final MainFrame main;
  
  /** The file name. */
  private final String fileName;

  /**
   * Instantiates a new load recent desc file event handler.
   *
   * @param frame the frame
   * @param fileName the file name
   */
  public LoadRecentDescFileEventHandler(MainFrame frame, String fileName) {
    super();
    this.main = frame;
    this.fileName = fileName;
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    this.main.loadAEDescriptor(new File(this.fileName));
  }

}
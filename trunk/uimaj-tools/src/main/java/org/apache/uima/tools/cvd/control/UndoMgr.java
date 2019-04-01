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

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.apache.uima.tools.cvd.MainFrame;


/**
 * Undo manager for text area.
 */
public class UndoMgr extends UndoManager implements ActionListener {

  /** The main. */
  private final MainFrame main;

  /**
   * Instantiates a new undo mgr.
   *
   * @param frame the frame
   */
  public UndoMgr(MainFrame frame) {
    this.main = frame;
  }

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 7677701629555379146L;

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent arg0) {
    undo();
    if (!canUndo()) {
      this.main.setUndoEnabled(false);
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.undo.UndoManager#addEdit(javax.swing.undo.UndoableEdit)
   */
  @Override
  public synchronized boolean addEdit(UndoableEdit arg0) {
    this.main.setUndoEnabled(true);
    return super.addEdit(arg0);
  }

  /* (non-Javadoc)
   * @see javax.swing.undo.UndoManager#discardAllEdits()
   */
  @Override
  public synchronized void discardAllEdits() {
    super.discardAllEdits();
    this.main.setUndoEnabled(false);
  }

}
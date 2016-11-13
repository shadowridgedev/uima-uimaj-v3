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

package org.apache.uima.tools.util.gui;

import java.awt.Insets;

import javax.swing.JButton;

import org.apache.uima.tools.images.Images;

// TODO: Auto-generated Javadoc
/**
 * The Class ImageButton.
 */
public class ImageButton extends JButton {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -8908984308731809496L;

  /**
   * Instantiates a new image button.
   *
   * @param imageFile the image file
   */
  public ImageButton(String imageFile) {
    super(Images.getImageIcon(imageFile));
  }

  /* (non-Javadoc)
   * @see javax.swing.JComponent#getInsets()
   */
  @Override
  public Insets getInsets() {
    return new Insets(2, 2, 2, 2);
  }
}

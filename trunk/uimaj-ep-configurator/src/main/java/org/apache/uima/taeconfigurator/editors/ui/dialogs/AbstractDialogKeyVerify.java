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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;


/**
 * The Class AbstractDialogKeyVerify.
 */
public abstract class AbstractDialogKeyVerify extends AbstractDialog implements VerifyKeyListener {

  /**
   * Instantiates a new abstract dialog key verify.
   *
   * @param aSection the a section
   * @param title the title
   * @param description the description
   */
  protected AbstractDialogKeyVerify(AbstractSection aSection, String title, String description) {
    super(aSection, title, description);
  }

  /**
   * New labeled single line styled text.
   *
   * @param twoCol the two col
   * @param label the label
   * @param tip the tip
   * @return the styled text
   */
  protected StyledText newLabeledSingleLineStyledText(Composite twoCol, String label, String tip) {
    setTextAndTip(new Label(twoCol, SWT.NONE), label, tip);
    return newSingleLineStyledText(twoCol, tip);
  }

  /**
   * New single line styled text.
   *
   * @param parent the parent
   * @param tip the tip
   * @return the styled text
   */
  protected StyledText newSingleLineStyledText(Composite parent, String tip) {
    StyledText w = new StyledText(parent, SWT.SINGLE | SWT.BORDER);
    w.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    w.setToolTipText(tip);
    w.addListener(SWT.KeyUp, this);
    w.addVerifyKeyListener(this);
    w.addListener(SWT.MouseUp, this); // for paste operation
    return w;
  }

  /* (non-Javadoc)
   * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
   */
  @Override
  public void verifyKey(VerifyEvent event) {
    event.doit = true;
    errorMessageUI.setText("");
    if (verifyKeyChecks(event))
      return;
    event.doit = false;
    setErrorMessage("An invalid key press was ignored. Please try again.");
    return;
  }

  // overridden in methods needing other key checks
  /**
   * Default verify key checks.
   *
   * @param event the event
   * @return true, if successful
   */
  public boolean verifyKeyChecks(VerifyEvent event) {
    return true;
  }

}

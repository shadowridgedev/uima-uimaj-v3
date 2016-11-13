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

package org.apache.uima.taeconfigurator.editors.ui;

import org.apache.uima.taeconfigurator.editors.Form2Panel;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.ui.forms.IManagedForm;

// TODO: Auto-generated Javadoc
/**
 * The Class TypePage.
 */
public class TypePage extends HeaderPageWithSash {

  /** The type section. */
  private TypeSection typeSection;

  /** The type import section. */
  private TypeImportSection typeImportSection;

  /**
   * Instantiates a new type page.
   *
   * @param editor the editor
   */
  public TypePage(MultiPageEditor editor) {
    super(editor, "Type Definitions");
  }

  /**
   * Called by the framework to fill in the contents.
   *
   * @param managedForm the managed form
   */
  @Override
  protected void createFormContent(IManagedForm managedForm) {
    // always show same screen layout - user could dynamically switch
    managedForm.getForm().setText("Type System Definition");
    Form2Panel form2Panel = setup2ColumnLayout(managedForm, 60, 40);
    managedForm.addPart(typeSection = new TypeSection(editor, form2Panel.left));
    managedForm.addPart(typeImportSection = new TypeImportSection(editor, form2Panel.right));
    createToolBarActions(managedForm);
  }

  /**
   * Gets the type section.
   *
   * @return the type section
   */
  public TypeSection getTypeSection() {
    return typeSection;
  }

  /**
   * Gets the type import section.
   *
   * @return the type import section
   */
  public TypeImportSection getTypeImportSection() {
    return typeImportSection;
  }

}

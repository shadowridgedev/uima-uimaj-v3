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
 */package org.apache.uima.caseditor.editor.editview;

import java.util.Collection;
import java.util.HashSet;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.editor.fsview.ITypePaneListener;
import org.apache.uima.caseditor.editor.fsview.TypeCombo;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

// TODO: Auto-generated Javadoc
/**
 * The Class CreateFeatureStructureDialog.
 */
public class CreateFeatureStructureDialog extends IconAndMessageDialog {

  /** The title. */
  private final String title;

  /** The size label. */
  private Label sizeLabel;

  /** The size text. */
  private Text sizeText;

  /** The array size. */
  private int arraySize;

  /** The type system. */
  private final TypeSystem typeSystem;

  /** The super type. */
  private final Type superType;

  /** The is array size displayed. */
  private boolean isArraySizeDisplayed;

  /** The type selection. */
  private TypeCombo typeSelection;

  /** The selected type. */
  private Type selectedType;

  /** The filter types. */
  private Collection<Type> filterTypes;

  /**
   * Initializes a the current instance.
   *
   * @param parentShell the parent shell
   * @param superType the super type
   * @param typeSystem the type system
   */
  protected CreateFeatureStructureDialog(Shell parentShell, Type superType, TypeSystem typeSystem) {

    super(parentShell);

    this.superType = superType;

    this.typeSystem = typeSystem;

    if (!superType.isArray()) {
      title = "Choose type";
      message = "Please choose the type to create.";
    } else {
      title = "Array size";
      message = "Please enter the size of the array.";
    }

    filterTypes = new HashSet<Type>();
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_ARRAY_BASE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_BYTE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_ANNOTATION_BASE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_SHORT));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_LONG));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_FLOAT));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_DOUBLE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_BOOLEAN));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_FLOAT));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_INTEGER));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_SOFA));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_STRING));
  }


  /* (non-Javadoc)
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    newShell.setText(title);
  }

  /**
   * Enable size enter.
   *
   * @param parent the parent
   */
  private void enableSizeEnter(Composite parent) {

    if (!isArraySizeDisplayed) {

      sizeLabel = new Label(parent, SWT.NONE);
      sizeLabel.setText("Size:");

      GridData sizeLabelData = new GridData();
      sizeLabelData.horizontalAlignment = SWT.LEFT;
      sizeLabel.setLayoutData(sizeLabelData);

      sizeText = new Text(parent, SWT.BORDER);

      GridData sizeTextData = new GridData();
      sizeTextData.grabExcessHorizontalSpace = true;
      sizeTextData.horizontalAlignment = SWT.FILL;
      sizeText.setLayoutData(sizeTextData);

      sizeText.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent event) {
          try {
            arraySize = Integer.parseInt(sizeText.getText());
          } catch (NumberFormatException e) {
            arraySize = -1;
          }
        }
      });

      isArraySizeDisplayed = true;
    }
  }

  /**
   * Disable size enter.
   */
  private void disableSizeEnter() {

    if (isArraySizeDisplayed) {
      sizeLabel.dispose();
      sizeText.dispose();
      isArraySizeDisplayed = false;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(final Composite parent) {

    createMessageArea(parent);

    final Composite labelAndText = (Composite) super.createDialogArea(parent);
    ((GridLayout) labelAndText.getLayout()).numColumns = 1;

    GridData labelAndTextData = new GridData(GridData.FILL_BOTH);
    labelAndTextData.horizontalSpan = 2;
    labelAndText.setLayoutData(labelAndTextData);

    if (!superType.isArray()) {
      
      Composite typePanel = new Composite(labelAndText, SWT.NULL);
      
      GridLayout typePanelLayout = new GridLayout();
      typePanelLayout.numColumns = 2;
      typePanel.setLayout(typePanelLayout);
      
      Label typeLabel = new Label(typePanel, SWT.NONE);
      typeLabel.setText("Type: ");
      
      typeSelection = new TypeCombo(typePanel);
      typeSelection.setInput(superType, typeSystem, filterTypes);

      selectedType = typeSelection.getType();

      // maybe consider to show the type of the array and disable the selector
      GridData typeSelectionData = new GridData();
      typeSelectionData.horizontalSpan = 1;
      typeSelectionData.horizontalAlignment = SWT.FILL;
      typeSelectionData.grabExcessHorizontalSpace = true;

      typeSelection.setLayoutData(typeSelectionData);

      typeSelection.addListener(new ITypePaneListener() {
        @Override
        public void typeChanged(Type newType) {
          selectedType = newType;

          if (newType.isArray()) {
            enableSizeEnter(labelAndText);
          } else {
            disableSizeEnter();
          }

          parent.pack(true);
        }
      });
    }

    if (superType.isArray()) {
      enableSizeEnter(labelAndText);
    }

    return labelAndText;
  }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Create", true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.IconAndMessageDialog#getImage()
   */
  @Override
  protected Image getImage() {
    return getShell().getDisplay().getSystemImage(SWT.ICON_QUESTION);
  }

  /**
   * Gets the array size.
   *
   * @return the array size
   */
  int getArraySize() {
    return arraySize;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  Type getType() {
    return selectedType;
  }
}

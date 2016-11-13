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

package org.apache.uima.tools.stylemap;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;

// TODO: Auto-generated Javadoc
/**
 * The Class LabelCellEditor.
 */
public class LabelCellEditor extends DefaultCellEditor {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 7810191917704574202L;

  /**
   * Instantiates a new label cell editor.
   */
  public LabelCellEditor() {
    super(new JTextField());
  }

  /* (non-Javadoc)
   * @see javax.swing.DefaultCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
   */
  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
          int row, int column) {
    JTextField textField = (JTextField) getComponent();
    if (isSelected)
      textField.selectAll();

    return textField;
  }
}

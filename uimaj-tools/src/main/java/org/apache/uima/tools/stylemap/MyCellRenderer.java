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
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.uima.tools.images.Images;

// TODO: Auto-generated Javadoc
/**
 * The Class MyCellRenderer.
 */
// cell renderer for the JTable
class MyCellRenderer extends DefaultTableCellRenderer {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 8130948041146818381L;

  /** The small arrow icon. */
  private ImageIcon smallArrowIcon = Images.getImageIcon(Images.SMALL_ARROW);

  /**
   * Gets the table cell renderer component.
   *
   * @param table the table
   * @param value the value
   * @param isSelected the is selected
   * @param hasFocus the has focus
   * @param row the row
   * @param column the column
   * @param styleList the style list
   * @return the table cell renderer component
   */
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
          boolean hasFocus, int row, int column, ArrayList styleList) {
    Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
            column);

    StyleMapEntry e = (StyleMapEntry) styleList.get(row);
    if (e != null) {
      int modelColumnNr = table.convertColumnIndexToModel(column);
      // if (modelColumnNr == StyleConstants.FEATURE_VALUE_COLUMN &&
      // !table.getModel().isCellEditable(row, modelColumnNr))
      // {
      // cell.setBackground(table.getParent().getBackground());
      // ((JLabel) cell).setIcon(null);
      // }
      // else
      {
        ((JLabel) cell).setIcon(((modelColumnNr == 0 && isSelected) ? smallArrowIcon : null));
        cell.setForeground(e.getForeground());
        cell.setBackground(e.getBackground());
      }
    }

    return cell;
  }
}

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

package org.apache.uima.tools.cvd;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;

import org.apache.uima.util.FileUtils;

// TODO: Auto-generated Javadoc
/**
 * Simple file viewer for viewing log files.
 * 
 * 
 */
public class LogFileViewer extends JFrame {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 3599235286749804258L;

	/** The log file. */
	private File logFile;

	/** The scroll pane. */
	private JScrollPane scrollPane;

	/** The text area. */
	private JTextArea textArea;

	/**
	 * Instantiates a new log file viewer.
	 *
	 * @throws java.awt.HeadlessException the java.awt. headless exception
	 */
	public LogFileViewer() {
		super();
	}

	/**
	 * Instantiates a new log file viewer.
	 *
	 * @param arg0 the arg 0
	 */
	public LogFileViewer(GraphicsConfiguration arg0) {
		super(arg0);
	}

	/**
	 * Instantiates a new log file viewer.
	 *
	 * @param arg0 the arg 0
	 * @throws java.awt.HeadlessException the java.awt. headless exception
	 */
	public LogFileViewer(String arg0) {
		super(arg0);
	}

	/**
	 * Instantiates a new log file viewer.
	 *
	 * @param arg0 the arg 0
	 * @param arg1 the arg 1
	 */
	public LogFileViewer(String arg0, GraphicsConfiguration arg1) {
		super(arg0, arg1);
	}

	/**
	 * Inits the.
	 *
	 * @param file the file
	 * @param d the d
	 */
	public void init(File file, Dimension d) {
		createMenus();
		this.logFile = file;
		this.textArea = new JTextArea();
		// Copy
		Action copyAction = this.textArea.getActionMap().get(DefaultEditorKit.copyAction);
		copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
				InputEvent.CTRL_MASK));
		copyAction.setEnabled(true);
		this.scrollPane = new JScrollPane(this.textArea);
		this.setContentPane(this.scrollPane);
		this.scrollPane.setPreferredSize(d);
		boolean doneLoadingFile = loadFile();
		if (!doneLoadingFile) {
			this.dispose();
			return;
		}
		this.pack();
		this.setVisible(true);
	}

	/**
	 * Creates the menus.
	 */
	private void createMenus() {
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		JMenuItem reload = new JMenuItem("Reload Log File");
		reload.addActionListener(new ActionListener() {
			@Override
      public void actionPerformed(ActionEvent event) {
				loadFile();
			}
		});
		fileMenu.add(reload);
		JMenuItem exit = new JMenuItem("Close Window");
		exit.addActionListener(new ActionListener() {
			@Override
      public void actionPerformed(ActionEvent event) {
				LogFileViewer.this.dispose();
			}
		});
		fileMenu.add(exit);
	}

	/**
	 * Load file.
	 *
	 * @return true, if successful
	 */
	private boolean loadFile() {
		if (!this.logFile.exists()) {
			JOptionPane.showMessageDialog(this, "The log file \"" + this.logFile.getAbsolutePath()
					+ "\" does not exist (yet).\nThis probably just means that nothing was logged yet.",
					"Information", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		String text = null;
		try {
			text = FileUtils.file2String(this.logFile, "UTF-8");
		} catch (IOException e) {
			handleException(e);
			return false;
		}
		this.textArea.setText(text);
		return true;
	}

	/**
	 * Handle exception.
	 *
	 * @param e the e
	 */
	protected void handleException(Exception e) {
		boolean hasAsserts = false;
		// assert(hasAsserts = true);
		if (hasAsserts) {
			e.printStackTrace();
		}
		JOptionPane.showMessageDialog(this, e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);

	}

}

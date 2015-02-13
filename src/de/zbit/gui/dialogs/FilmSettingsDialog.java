/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */

package de.zbit.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import de.zbit.data.Signal.SignalType;
import de.zbit.gui.GUITools;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.PathwaySelector;
import de.zbit.util.Species;
import de.zbit.visualization.VisualizeTimeSeries;

/**
 * This dialog is shown before a film for mRNA time series visualization
 * is generated. It asks the user following parameters: <br>
 * - the pathway which should be visualized<br>
 * - the cutoff value for gene filtering<br>
 * - the frame rate and duration of the film.
 * @author Felix Bartusch
 * @version $Rev$
 */

public class FilmSettingsDialog extends JPanel {
	private static final long serialVersionUID = -5972049029827561451L;
	
	private VisualizeTimeSeries model;
	private JPanel buttons;
	private JLabel frameInTimeUnitLabel;
	private PathwaySelector pwSel = null;
	private JFormattedTextField cutoffTextField;

	private int numFrames = 10;
	private double cutoff;
	private double start;
	private double end;
	private String timeUnit;
	protected JPanel timePointPanel;
	private boolean justVisualizeData = false;

	public FilmSettingsDialog(Species species, VisualizeTimeSeries model) {
		this.model = model;
		this.start = model.getFirstTimePoint();
		this.end = model.getLastTimePoint();
		this.timeUnit = model.getTimeUnit();
		
		// the pathway part of the dialog
		try {
			pwSel = new PathwaySelector(Translator.getFunctionManager(),null, species.getKeggAbbr());
		} catch (Exception e) {
			GUITools.showErrorMessage(null, e);
			e.printStackTrace();	
		}
		
		// the gene filter part
		JComponent filterSettings = filterPanel();

		// the film settings part
		JComponent filmSettings = filmSettingsPanel();
		
		// build one panel with the two settings panels
		JPanel settings = new JPanel(new GridLayout(1,2));
		settings.add(filterSettings);
		settings.add(filmSettings);
		
		this.buttons = GUITools.buildOkCancelButtons(this);
		this.setLayout(new BorderLayout());
			
		add(pwSel, BorderLayout.NORTH);
		add(settings, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		
		setVisible(true);
	}
	
	/**
	 * Creates a panel where the user can choose a cutoff value.
	 * @return the panel
	 */
	private JComponent filterPanel() {
		// pValue or FC?
		final boolean isPValue = model.getSignalType() == SignalType.pValue ? true : false;
		
		JLabel cutoffLabel = new JLabel("Cutoff value");	
		// default value. 
		cutoff = isPValue ? 0.05 : 1;	
		String toolTip = "";
		if(isPValue)
			toolTip = "Filter genes with a p-value higher than the cutoff value.";
		else
			toolTip = "Filter genes with an absolute fold change lower than cutoff value.";
		
		// Ensure that '.' is decimal seperator. Otherwise you cannot parse the text to double
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(getLocale());
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(','); 
		
		DecimalFormat format = new DecimalFormat("###.##", otherSymbols);
		cutoffTextField = new JFormattedTextField(format);
		cutoffTextField.setText(String.valueOf(cutoff));
		cutoffTextField.setToolTipText(toolTip);
		
		// build resulting panel
		JComponent cutoffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		cutoffPanel.add(cutoffLabel);
		cutoffPanel.add(cutoffTextField);
		cutoffPanel = GUITools.createTitledPanel(cutoffPanel, "Filter settings");
		
		return cutoffPanel;
	}

	/**
	 * Creates a panel with textfields to fill in <br>
	 * - frame rate<br>
	 * - duration (in seconds)<br>
	 * of the film 
	 * @return the panel
	 */
	private JComponent filmSettingsPanel() {
		// Let the user choose what time points will be visualized
		JRadioButton equidistantButton = new JRadioButton("Equidistant frames");
		equidistantButton.setToolTipText("The visualized time points have the same difference in time. You can choose the number of frames.");
		
		JRadioButton eachTimePointOneFrameButton = new JRadioButton("Visualize data time points");
		eachTimePointOneFrameButton.setToolTipText("Each time point of the data is visualized in one frame.\nNumber of observations is the number of frames.");
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(equidistantButton);
		bg.add(eachTimePointOneFrameButton);
		
		// initialize the panel
		bg.setSelected(equidistantButton.getModel(), true);
		
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(equidistantButton, BorderLayout.NORTH);
		buttonPanel.add(eachTimePointOneFrameButton, BorderLayout.CENTER);
	
		// 
		// build frame selector
//		JLabel rateTextLabel = new JLabel("Frames per second");
//		String[] frameRates = {"1","2","3","4","5","10","15","20"};
//		JComboBox<String> rateComboBox = new JComboBox<String>(frameRates);
//		rateComboBox.addActionListener(new ActionListener() {	
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				JComboBox<Integer> source = (JComboBox<Integer>) e.getSource();
//				frameRate = Integer.parseInt((String) source.getSelectedItem());
//				updateTimeUnitInSeconds();
//			}
//		});	
//		rateComboBox.setSelectedIndex(5);
//		rateComboBox.setEnabled(true);
//		// Place text area left of the combo box
//		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
//		panel1.add(rateTextLabel);
//		panel1.add(rateComboBox);
			
		// Build the panel to select the number of frames
		// (which determines the time points to visualize)
		timePointPanel = new JPanel(new CardLayout());
		timePointPanel.add(buildNumFramePanel()); // The user can choose the number of frames with this panel
		timePointPanel.add(new JPanel()); // The empty panel for the data visualization without interpolation
		
		// build the resulting film settings panel
		JPanel res = new JPanel(new BorderLayout());
		res.add(buttonPanel, BorderLayout.CENTER);
		res.add(timePointPanel, BorderLayout.SOUTH);
		
		// Put some action in the button's life
		equidistantButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				// Show a text field for the number of frames
				JRadioButton source = (JRadioButton) e.getSource();

				// Just change panel, if the button is selected
				if(source.getModel().isSelected()) {
					CardLayout cl = (CardLayout) timePointPanel.getLayout();

					// Show the timePointPanel
					cl.first(timePointPanel); // because the panel was added first in the constructor
					justVisualizeData = false;
				}
			}
		});

		eachTimePointOneFrameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Remove the equidistant panel (set an empty panel)
				JRadioButton source = (JRadioButton) e.getSource();

				// Just change panel, if the button is selected
				if(source.getModel().isSelected()) {
					CardLayout cl = (CardLayout) timePointPanel.getLayout();

					// Show the empty timePointPanel
					cl.last(timePointPanel); // because the panel was added at last in the constructor
					justVisualizeData = true;
				}
			}
		});

		return GUITools.createTitledPanel(res, "Film settings");
	}

	protected JPanel buildNumFramePanel() {
		// Show a panel with a text field. The user can choose the number of frames
		// with the text field
		// a time unit corresponds to how many seconds?
		frameInTimeUnitLabel = new JLabel("Test");
		
		// initialize timeUnitInsSecondsTextArea
		updateFrameInTimeUnits();

		// build numFrame field
		JLabel numFrameLabel = new JLabel("Number of frames");

		JTextField numFrameTextField = new JTextField(String.valueOf(numFrames));
		numFrameTextField.getDocument().addDocumentListener(new DocumentListener() {     
			@Override
			public void removeUpdate(DocumentEvent e) {
				Document source = e.getDocument();
				updateNumFrame(source);
				updateFrameInTimeUnits();   
			}
			// get value of the textfield. Set Value to -1 if the text isn't castable to int.
			private void updateNumFrame(Document source) {
				try {
					String s = source.getText(0, source.getLength());
					numFrames = Integer.parseInt(s);
				} catch (Exception e) {
					numFrames = -1;
				}		 
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				Document source = e.getDocument();
				updateNumFrame(source);
				updateFrameInTimeUnits();              
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				Document source = e.getDocument();
				updateNumFrame(source);
				updateFrameInTimeUnits();          
			}
		});

		// Place text area left of duration text field
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p.add(numFrameLabel);
		p.add(numFrameTextField);
		
		JPanel res = new JPanel(new BorderLayout());
		res.add(p, BorderLayout.CENTER);
		res.add(frameInTimeUnitLabel, BorderLayout.SOUTH);
		
		return res;
	}

	/**
	 * Update the FrameInTimeUnit label.
	 * Shows the user how many time units correspond to one frame.
	 */
	protected void updateFrameInTimeUnits() {
		String message = "";
		
		// compute displayed text
		double diff = end - start; 
		numFrames = getNumFrames();
		double oneFrame = diff / (numFrames -1); // 1 Frame corresponds to xxx [time unit]
		String s = String.format("%.1f", oneFrame); // x.x, just one place after comma
		
		// display computed text
		message = " 1 Frame" + " \u2259 " + s + timeUnit;
		frameInTimeUnitLabel.setText(message);
		frameInTimeUnitLabel.repaint();
	}

	/**
	 * @return true, if ok button was pressed
	 */
	public boolean okPressed() {
		return ((JButton) ((JPanel) buttons.getComponent(0)).getComponent(0)).isSelected();
	}

	/**
	 * @return true, if cancel button was pressed
	 */
	public boolean cancelPressed() {
		return !okPressed();
	}

	/**
	 * @return the selected pathway id
	 */
	public String getSelectedPathwayID() {
		return pwSel.getSelectedPathwayID();
	}
	
	/**
	 * @return the selected frame rate
	 */
	public int getFrameRate() {
		// Don't need frameRate any more
		return 1;
	}

	/**
	 * @return the number of frames
	 */
	public int getNumFrames() {
		return numFrames;
	}
	
	/**
	 * @return the cutoff value
	 */
	public double getCutoff() {
		return Math.abs(Double.parseDouble(cutoffTextField.getText()));
	}
	
	/**
	 * @return the selected radio button
	 */
	public boolean getJustVisualizeDate() {
		return justVisualizeData;
	}
}

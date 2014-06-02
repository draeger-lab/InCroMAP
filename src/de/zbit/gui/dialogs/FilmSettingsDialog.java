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
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import de.zbit.visualization.VisualizeTimeSeriesListener;

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
	private JLabel timeUnitInSecondsTextArea;
	private PathwaySelector pwSel = null;
	private JFormattedTextField cutoffTextField;

	private int frameRate;
	private int duration = 30;
	private double cutoff;
	private double start;
	private double end;
	private String timeUnit;

	public FilmSettingsDialog(Species species, VisualizeTimeSeriesListener controller, VisualizeTimeSeries model) {
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
		cutoff = isPValue ? 0.5 : 1;	
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
		// a time unit corresponds to how many seconds?
		timeUnitInSecondsTextArea = new JLabel("Test");
		
		// build duration field
		JLabel durationLabel = new JLabel("Duration in seconds");
		//DecimalFormat format = new DecimalFormat("###");
		//durationTextField = new JFormattedTextField(format);
		//durationTextField.setText(String.valueOf(duration));
		//durationTextField.setToolTipText("The duration of the resulting film.");
		
		// build frame selector
		JLabel rateTextLabel = new JLabel("Frames per second");
		String[] frameRates = {"1","2","3","4","5","10","15","20"};
		JComboBox<String> rateComboBox = new JComboBox<String>(frameRates);
		rateComboBox.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox<Integer> source = (JComboBox<Integer>) e.getSource();
				frameRate = Integer.parseInt((String) source.getSelectedItem());
				updateTimeUnitInSeconds();
			}
		});	
		rateComboBox.setSelectedIndex(5);
		rateComboBox.setEnabled(true);
		// Place text area left of the combo box
		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel1.add(rateTextLabel);
		panel1.add(rateComboBox);
			
		JTextField durationTextField = new JTextField(String.valueOf(duration));
		durationTextField.getDocument().addDocumentListener(new DocumentListener() {     
			@Override
			public void removeUpdate(DocumentEvent e) {
				Document source = e.getDocument();
				updateDuration(source);
				updateTimeUnitInSeconds();   
			}
			// get value of the textfield. Set Value to -1 if the text isn't castable to int.
			private void updateDuration(Document source) {
				 try {
					String s = source.getText(0, source.getLength());
					duration = Integer.parseInt(s);
				} catch (Exception e) {
					duration = -1;
				}		 
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				Document source = e.getDocument();
				updateDuration(source);
				updateTimeUnitInSeconds();              
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				Document source = e.getDocument();
				updateDuration(source);
				updateTimeUnitInSeconds();          
			}
		});
				
		// Place text area left of duration text field
		JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel2.add(durationLabel);
		panel2.add(durationTextField);
		
		// build the resulting film settings panel
		JPanel res = new JPanel(new BorderLayout());
		res.add(panel1, BorderLayout.NORTH);
		res.add(panel2, BorderLayout.CENTER);
				
		// initialize timeUnitInsSecondsTextArea
		updateTimeUnitInSeconds();
		res.add(timeUnitInSecondsTextArea, BorderLayout.SOUTH);
		
		return GUITools.createTitledPanel(res, "Film settings");
	}

	/**
	 * Update the timeUnitInSecondsTextArea text area.
	 * Shows the user how many seconds correspond to one time unit.
	 */
	protected void updateTimeUnitInSeconds() {
		String message = "";
		
		// illegal text in duration text field
//		if(duration == -1)
//			message = "Just decimal numbers are accepted in the duration text field";
		
		// compute displayed text
		double diff = end - start; 
		duration = getDuration();
		double oneTimeUnit = duration / diff; // 1 [time unit] corresponds to ...
		String s = String.format("%.1f", oneTimeUnit); // x.x, just one place after comma
		
		// display computed text
		message = "1 " + timeUnit + " \u2259 " + s + " seconds.";
		timeUnitInSecondsTextArea.setText(message);
		timeUnitInSecondsTextArea.repaint();
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
		return frameRate;
	}

	/**
	 * @return the selected duration
	 */
	public int getDuration() {
		//return Integer.parseInt(durationTextField.getText());
		return duration;
	}
	
	/**
	 * @return the cutoff value
	 */
	public double getCutoff() {
		return Math.abs(Double.parseDouble(cutoffTextField.getText()));
	}
}

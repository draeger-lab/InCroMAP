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

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import de.zbit.gui.GUITools;
import de.zbit.gui.tabs.NSTimeSeriesTab;
import de.zbit.math.TimeSeriesModel;


/**
 * This dialog is shown before a model for mRNA time series is computed. It asks the user following settings: <br>
 * - If the time points are exponentially distributed<br>
 * - a cutoff value. Just genes that are at least at one time point better than the cutoff are modelled<br>
 * @author Felix Bartusch
 * @version $Rev$
 */

public class ModelSettingsDialog extends JPanel {
	/**
   * 
   */
  private static final long serialVersionUID = -368982249627600338L;

  /**
	 * The textfield for the cutoff value.
	 */
	private JFormattedTextField cutoffTextField;
	
	/**
	 * Label showing how many genes will remain after filtering
	 */
	private JLabel remainsLabel;
	
	/**
	 * Checkbox for exponentially distributed time points.
	 */
	private JCheckBox distButton;
	
	/**
	 * The parent {@link de.zbit.gui.tabs.NSTimeSeriesTab}.
	 */
	private NSTimeSeriesTab parent;
	
	/**
	 * The individual panel for each model method.
	 */
	private JComponent individualPanel;
	
	/**
	 * Ok and Cancel buttons
	 */
	private JPanel buttons;
	
	
	/**
	 * Generate a new model settings dialog.
	 * @param parent
	 */
	public ModelSettingsDialog(NSTimeSeriesTab parent, TimeSeriesModel method, double cutoffGuess) {
		this.parent = parent;
		
		// The time points are exponentially distributed part
		JComponent distributionPanel = generateDistributionPanel();
		
		// The gene filter part
		JComponent filterPanel = generateFilterPanel(cutoffGuess);
		
		// The special part for each model method (e.g. number of iterations for TimeFit)
		individualPanel = method.getIndividualSettingsPanel();
		
		// A panel common to all model methods
		JComponent commonPanel = new JPanel(new GridLayout(2,1));
		commonPanel.add(distributionPanel);
		commonPanel.add(filterPanel);
		
		// If there is an individualPanel, add it to the dialog
		if(individualPanel == null) {
			this.setLayout(new GridLayout(1,1));
			add(commonPanel);
		} else {
			this.setLayout(new GridLayout(2,1));
			add(commonPanel);
			add(individualPanel);
		}
		
		setVisible(true);
	}
	
	
	/**
	 * Creates a panel where the user can choose a cutoff value.
	 * @param cutoffGuess Value in the cutoff text field at the beginning
	 * @return the panel
	 */
	private JComponent generateFilterPanel(double cutoffGuess) {
		// Add a listener to the text field. If the content is changed, update the remainsLabel.
		// Label showing number of genes remaining after filtering
		remainsLabel = new JLabel();
		
		// pValue or FC?
		final boolean isPValue = parent.isPValue();
	
		JLabel cutoffLabel = new JLabel("Cutoff value");	
		// Initial guess of the cutoff value. 
		double cutoff = cutoffGuess;	
		String toolTip = "";
		if(isPValue)
			toolTip = "Do not model genes where no p-value is lower than the cutoff.";
		else
			toolTip = "Do not model genes where no absolute value of the fold change is higher than the cutoff.";
		
		// Ensure that '.' is decimal seperator. Otherwise you cannot parse the text to double
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(getLocale());
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(','); 
		
		DecimalFormat format = new DecimalFormat("###.##", otherSymbols);
		cutoffTextField = new JFormattedTextField(format);
		cutoffTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				setGenesRemainingText(e);
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				setGenesRemainingText(e);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				setGenesRemainingText(e);
			}
			
			public void setGenesRemainingText(DocumentEvent e) {
				// Get the double value of the cutoff text field
				String text = "1.0";
				double value = 0.0;
				try {
					text = e.getDocument().getText(0, e.getDocument().getLength());
				} catch (BadLocationException e1) {
				}
				try{
					value = Double.valueOf(text);
				} catch(NumberFormatException ex) {
					value = 0.0;
				}
				
				// How many genes will remain after filtering?
				int remainingGenes = parent.genesRemainingAfterFiltering(value);
				
				// Set the new text
				remainsLabel.setText(remainingGenes + " genes will be modelled after filtering.");
			}
		});
		
		cutoffTextField.setText(String.valueOf(cutoff));
		cutoffTextField.setToolTipText(toolTip);
		
		// build resulting panel
		//JComponent cutoffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JComponent cutoffPanel = new JPanel(new GridLayout(2,2));
		cutoffPanel.add(cutoffLabel);
		cutoffPanel.add(cutoffTextField);
		cutoffPanel.add(remainsLabel);
		cutoffPanel = GUITools.createTitledPanel(cutoffPanel, "Filter settings");
		
		return cutoffPanel;
	}
	
	
	/**
	 * A Panel containing a checkbox
	 * @return the panel
	 */
	private JComponent generateDistributionPanel() {
		// Checkbox - User shall mark this box if the time points are exponentially distributed
		distButton = new JCheckBox("Select if the time points are exponentially distributed");

		JComponent distPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		distPanel.add(distButton);
		distPanel = GUITools.createTitledPanel(distPanel, "Time points distribution");
		
		return distPanel;
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
	 * Get the cutoff value.
	 */
	public double getCutoff() {
		return Math.abs(Double.parseDouble(cutoffTextField.getText()));
	}
	
	/**
	 * Is the checkbox for exponentially distributed time points toggled?
	 */
	public boolean isExponentiallyDistributed() {
		return distButton.isSelected();
	}
}
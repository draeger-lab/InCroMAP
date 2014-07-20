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

package de.zbit.gui.customcomponents;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.zbit.gui.GUITools;
import de.zbit.visualization.VisualizeTimeSeriesListener;
import de.zbit.visualization.VisualizeTimeSeriesListener.VTSAction;

/**
 * A {@link JPanel} with play-, pause-, backwards-, forwards-buttons, a slider and some
 * labels with information.
 * @author Felix Bartusch
 * @version $Rev$
 */

public class FilmControlPanel extends JPanel {
	private static final long serialVersionUID = -4441791127291850221L;

	private ActionListener listener;
	private JButton prevButton, playButton, pauseButton, nextButton;
	private JSlider slider;
	private JLabel frameToTimeUnit;
	private JPanel playPausePanel;

	/**
	 * Build a standard FilmControlPanel with buttons, slider and some information labels.
	 * @param controller which reacts on user input
	 */
	public FilmControlPanel(VisualizeTimeSeriesListener controller) {
		super();
		this.listener = controller;

		//The backward button
		prevButton = GUITools.createButton(UIManager.getIcon("ICON_BACKW_32"), controller,
				VTSAction.SHOW_PREV_FRAME, VTSAction.SHOW_PREV_FRAME.getToolTip());

		// The play button
		playButton = GUITools.createButton(UIManager.getIcon("ICON_PLAY_32"), controller,
				VTSAction.PLAY_FILM, VTSAction.PLAY_FILM.getToolTip());

		// The pause button
		pauseButton = GUITools.createButton(UIManager.getIcon("ICON_PAUSE_32"), controller,
				VTSAction.PAUSE_FILM, VTSAction.PAUSE_FILM.getToolTip());

		// The forward button
		nextButton = GUITools.createButton(UIManager.getIcon("ICON_FORW_32"), controller,
				VTSAction.SHOW_NEXT_FRAME, VTSAction.SHOW_NEXT_FRAME.getToolTip());

		// The panel containing the play / pause button
		JPanel playPanel = new JPanel();
		playPanel.add(playButton);

		JPanel pausePanel = new JPanel();
		pausePanel.add(pauseButton);

		playPausePanel = new JPanel(new CardLayout());
		playPausePanel.add(playPanel);
		playPausePanel.add(pausePanel);

		// The slider. Get the duration of the film.		
		slider = new JSlider(1, controller.getNumFrames());

		// Add a ChangeListener to the slider. The listener should show then the
		// frame occuring at the chosen position
		slider.addChangeListener(new ChangeListener() {		
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if (!source.getValueIsAdjusting()) {
					int val = (int)source.getValue();
					listener.actionPerformed(new ActionEvent(this, val, VTSAction.GO_TO_POSITION.toString()));
				}    
			}
		});

		// Label on the top of the slider
		JLabel sliderLabelSecond = new JLabel("Jump to frame", JLabel.CENTER);
		sliderLabelSecond.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Change sliders ticks
		slider.setMajorTickSpacing(10);
		slider.setMinorTickSpacing(2);
		slider.setPaintTicks(true);

		// Place the two things in a new panel
		JPanel sliderPanel = new JPanel(new BorderLayout());
		sliderPanel.add(sliderLabelSecond, BorderLayout.NORTH);
		sliderPanel.add(slider, BorderLayout.CENTER);

		// Add a label, which tells the user what value of the time unit corresponds to the current
		// visualized second
		frameToTimeUnit = new JLabel();

		// Add buttons and slider to this panel
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(prevButton);
		//add(playButton);
		add(playPausePanel);
		add(nextButton);
		add(sliderPanel);
		add(frameToTimeUnit);
	}

	/**
	 * Enables / Disables the next frame button
	 * @param b if true, enable the next frame button. Otherwise disable next frame button
	 */
	public void enableNextButton(boolean b) {
		nextButton.setEnabled(b);		
	}

	/**
	 * Enables / Disables the previous frame button
	 * @param b if true, enable the next frame button. Otherwise disable next frame button
	 */
	public void enablePrevButton(boolean b) {
		prevButton.setEnabled(b);		
	}

	/**
	 * Set the text of the frameToTimeUnit label.
	 * @param frame, the current visualized frame
	 * @param timePoint, the current visualized time point
	 * @param timeUnitString, the string which describes the time unit (e.g. "s" for seconds)
	 */
	public void setFrameToTimeUnit(int frame, double timePoint, String timeUnitString) {
		String text = "Frame " + frame + " \u2259 " + String.format("%.2f", timePoint) + timeUnitString;
		frameToTimeUnit.setText(text);		
	}

	/**
	 * Switch between the play/pause-button.
	 * @param b if true, show the play button. Otherwise show the pause button.
	 */
	public void enablePlayButton(boolean enablePlay) {

		// With a card layout it is very easy to change the buttons
		// http://docs.oracle.com/javase/tutorial/uiswing/layout/card.html
		CardLayout cl = (CardLayout) playPausePanel.getLayout();

		// Show the play button if enablePlay is true.
		if(enablePlay) {
			cl.first(playPausePanel); // because the play panel was added first in the constructor
		} else {
			cl.last(playPausePanel); // because the pause panel was added at last in the constructor
		}
	}

	/**
	 * Change the actual value of the slider.
	 * @param curFrame
	 */
	public void setSliderValue(int curFrame) {
		// Is the new value in the range of possible values?
		if(curFrame >= slider.getMinimum() && curFrame <= slider.getMaximum())
			slider.setValue(curFrame);

		// Otherwise, do nothing
	}
}

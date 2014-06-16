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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
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
	private boolean isFilmRunning = false;
	
	
	
	
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

		// The forward button
		nextButton = GUITools.createButton(UIManager.getIcon("ICON_FORW_32"), controller,
				VTSAction.SHOW_NEXT_FRAME, VTSAction.SHOW_NEXT_FRAME.getToolTip());

		// The pause button
		pauseButton = GUITools.createButton(UIManager.getIcon("ICON_PAUSE_32"), controller,
				VTSAction.PAUSE_FILM, VTSAction.PAUSE_FILM.getToolTip());

		// The slider. Get the duration of the film.		
		slider = new JSlider(0, controller.getDuration());
		
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
		

		// Add buttons and slider to this panel
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(prevButton);
		add(playButton);
		add(nextButton);
		add(slider);
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
}

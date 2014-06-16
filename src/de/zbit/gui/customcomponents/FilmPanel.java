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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This panel displays the {@link Graphics2D} images of a film.
 * @author Felix Bartusch
 * @version $Rev$
 */
public class FilmPanel extends JPanel implements MouseWheelListener{
	private static final long serialVersionUID = -6620157225711675537L;
	
	/**
	 * Label, which has the curImage as icon
	 */
	private JLabel curFrame;
	
	/**
	 * The original image
	 */
	private BufferedImage origImage;
	
	/**
	 * The resized image based on the original image and the zoomFactor
	 */
	private BufferedImage curImage;
	
	/**
	 * The original dimension of the input stream
	 */
	private Dimension origDimension = null;

	/**
	 * == 1 : frames are shown in original size</br>
	 *  < 1 : zoom out (frames have smaller size)</br>
	 *  > 1 : zoom in  (frames have bigger size)
	 */
	private double zoomFactor = 1;
	
	/**
	 * Create a new film panel which displays images with a certain dimension.
	 * @param dimension
	 */
	public FilmPanel(Dimension origDimension) {
		super();
		curFrame = new JLabel();
		this.add(curFrame);
		this.origDimension = origDimension;
		System.out.println("Orig Dimension in film Panel: " + origDimension);
		validate();
	}

	/**
	 * Show the next Frame.
	 * @param nextFrame
	 */
	public void showNextFrame(BufferedImage nextFrame) {
		// Set the original image
		this.origImage = nextFrame;
			
		// Width and height of the resized image
//		System.out.println("Zoom factor != 1 ? " + (zoomFactor != 1));
//		System.out.println("dimension null? " + (origDimension == null));
		int newWidth = (new Double(origDimension.getWidth() * zoomFactor)).intValue();
		int newHeight = (new Double(origDimension.getHeight() * zoomFactor)).intValue();
		
		// This should be the resized image
		BufferedImage nextFrameResized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

		// g is used to draw the resized image
		Graphics2D g = nextFrameResized.createGraphics();

		// Resize image
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(nextFrame, 0, 0, newWidth, newHeight, null);
		
		// Show the next frame
		curImage = nextFrameResized;
		ImageIcon imageIcon = new ImageIcon(curImage);
		curFrame.setIcon(imageIcon);
		validate(); // just call validate() in the scrollPane?
	}
	
	/**
	 * Set the zoom factor and show the next frame with this zoom factor.
	 * @param nextFrame The next frame to visualize
	 * @param zoomFactor The new zoom factor
	 */
	public void showNextFrame(BufferedImage nextFrame, double zoomFactor) {
		// Set the new zoom factor
		this.zoomFactor = zoomFactor;
		
		// Show the next frame
		showNextFrame(nextFrame);
	}
	
	/**
	 * Tests if this panel and his component is valid
	 */
	public void isPanelValid() {
		System.out.println("Is label valid? " + curFrame.isValid());
		System.out.println("Is panel valid? " + this.isValid());
	}
	
	/**
	 * Print the dimension of the current Image
	 */
	public void printImageDimension() {
		System.out.println("Image width: " + curImage.getWidth());
		System.out.println("Image height: " + curImage.getHeight());
	}
	
	// Zoom the image in/out. For that, change the zoomFactor and generate a new image.
	// One elementary 
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		
		// zoom into the image? i.e. enlarge the image
		boolean zoomIn = e.getWheelRotation() < 0;
		
		// How does the zoomFactor changes? Every wheelRotation +/- 5% (i.e. 0.05)
		double zoomChange = zoomIn ? 0.05 : -0.05;
		zoomFactor += zoomChange;
		
		// Don't allow a negative zoomFactor!
		if(zoomFactor <= 0)
			zoomFactor = 0.05;
		
		// Don't allow a high zoomFactor. Image gets toooo big.
		if(zoomFactor > 1.5)
			zoomFactor = 1.5;
		
		// show the same frame, but now with another zoomFactor
		showNextFrame(origImage);
	}
}

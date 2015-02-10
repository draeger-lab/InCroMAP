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

package de.zbit.math;

import java.awt.GridLayout;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.jfree.util.Log;

import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.gui.GUITools;
import de.zbit.gui.tabs.NSTimeSeriesTab;
import de.zbit.util.objectwrapper.ValueTriplet;
import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * Implementation of the TimeFit algorithm to compute a continous representation
 * of time series data. This class just computes the parameters of the single gene models.
 * The computed parameters are later used to define the model for each single gene.
 * So this class does not implement the @link{de.zbit.math.TimeFit.computeValueAtTimePoint(double)} method.
 * See: Bar-Joseph, Z. et al. (2003).
 * 			Continuous representations of time-series gene expression data.
 * 			Journal of Computational Biology.
 *
 * @see <a href="http://www.ncbi.nlm.nih.gov/pubmed/12935332">The PubMed entry of the paper</a>
 * 
 * @author Felix Bartusch
 * @version $Rev$
 */

// grep -H -R -l "^import y." ./

public class TimeFit extends TimeSeriesModel {
	/**
	 * Order of the basis polynomials (i.e. for cubic polynomial order = 4)
	 */
	int order = 4;

	/**
	 * The number of the spline control points.
	 */
	int q = -1;

	/**
	 * The control points of the spline
	 */
	ArrayList<Point2D> controlPoints;

	/**
	 * The columns of the expression matrix which belong to the control points
	 */
	int[] controlPointsColumn;

	/**
	 * The values of the basis B-splines at the control points.
	 * A m by q matrix, where m is the number of time points and q
	 * is the number of control points. The first row correspondends to
	 * the value of the basis B-splines for the first time point.
	 */
	RealMatrix s;

	/**
	 * The result of transposed s multiplied with s. Because it is used many times. 
	 */
	RealMatrix sts;

	/**
	 * The measured data for ALL genes.
	 * A row corresponds to a measured time points, a column corresponds to the data of one gene.
	 * To get the data of gene i as m x 1 array, use yMatrix.getColumnMatrix(i).
	 */
	RealMatrix yMatrix;

	/**
	 * The class centers. The center class i is given the i-th column of the mu matrix.
	 */
	RealMatrix mu;
	
	/**
	 * The mapping of a gene's position in the yMatrix to his class.
	 */
	int[] pos2class;
	
	/**
	 * The filtered data from which the model is generated.
	 */
	ArrayList<mRNATimeSeries> filteredData;

	/**
	 * The set of genes for a class.
	 */
	ArrayList<ArrayList<Integer>> class2genes;

	/**
	 * The covariance matrices Γ_j of the classes.
	 */
	ArrayList<RealMatrix> covMatrices;

	/**
	 * The list of the inverse covariance matrices.
	 */
	ArrayList<RealMatrix> inverseCovMatrices;

	/**
	 * The Gene specific variation coefficients. The j-nth matrix of the list contains the  variation
	 * coefficients for the j-th class.
	 * ...
	 * The rows of a matrix corresponds to genes and the columns
	 * corresponds to the variation coefficient of the control points. So a matrix has a dimension of
	 * numGenes by q.
	 */
	ArrayList<RealMatrix> gamma;


	/**
	 * The variances of the genes.
	 */
	double[] geneVariances;

	/**
	 * The variance.
	 */
	double variance = 1;

	/**
	 * The noise vectors for each gene. The noise vector for gene i is the i-th row of the matrix.
	 */
	RealMatrix noiseVectors;

	/**
	 * The knot vector of the B-spline. The number of knots has to fulfill the following equation
	 * #knots = q + order
	 */
	double[] knots;

	/**
	 * The 	probability that gene i belongs to class j, where row correspondens to genes and columns
	 * to classes. So probs[i][j] contains the probability, that gene i belongs to class j.
	 */
	double[][] probs;
	
	/**
	 * Threshold for the EM-algorithm. Tells EM-algorithm when to stop.
	 */
	double threshold = 1;
	
	/**
	 * Maximum number of iteration for the EM-algorithm.
	 */
	int maxIteration = 20;

	/**
	 * The class probability that are updated in the EM-algorithm.
	 */
	double[] classProbs;

	/**
	 * The log likelihood of the data given the model.
	 */
	double logLikelihood;

	/**
	 * The number of gene classes.
	 */
	int numClasses = 9;

	/**
	 * The number of genes.
	 */
	int numGenes;
	
	/**
	 * Was the model succesfully generated?
	 */
	boolean isModelled = false;
	
	/**
	 * Contains number of models chosen by the user
	 */
	private JFormattedTextField numModelTextField;

	/**
	 * Contains maximal iteration number per model chosen by the user
	 */
	private JFormattedTextField maxIterTextField;

	/**
	 * Contains number of classes chosen by the user
	 */
	private JFormattedTextField numClassesTextField;
	
	/**
	 * Constructor does nothing. So that an object of this class can be instanced by
	 * calling newInstance()
	 */
	public TimeFit() {
		// Intentially left blank.
	}
	
	/**
	 * @param numClasses The number of classes
	 * @param maxIteration The maximal number of iterations of the EM-algorithm
	 */
	public TimeFit(int numClasses, int maxIteration) {
		this.numClasses = numClasses;
		this.maxIteration = maxIteration;
	}


	/* (non-Javadoc)
	 * @see de.zbit.math.TimeSeriesModel#generateModel(de.zbit.data.mRNA.mRNATimeSeries, java.util.List)
	 */
	@Override
	public void generateModel(mRNATimeSeries dataPoints,
			List<ValueTriplet<Double, String, SignalType>> timePoints,
			double cutoff, boolean isExponentiallyDistributed) {
		// The model isn't build for one single gene but for all genes at the same time.
		// intentially left blank!
		return;
	}

	
	/**
	 * Generate the time series model for the data. Run a certain number of iterations and choose
	 * the best generated model among them. The data can also be filtered, so that just genes are modelled
	 * that suffice a cutoff value.
	 * @param data
	 * @param timePoints
	 * @param iterations Number of models to generate. The best model is then chosen.
	 * @param cutoff Consider just genes, that are at some time point greater than the cutoff value.
	 * @param isExponentiallyDistributed Are the time points exponentially distributed?
	 */
	public void generateModel(NSTimeSeriesTab parent, ArrayList<mRNATimeSeries> data,
			List<ValueTriplet<Double, String, SignalType>> timePoints,
			int iterations, double cutoff, boolean isExponentiallyDistributed,
			AbstractProgressBar pb) {
		
		this.isExponentiallyDistributed = isExponentiallyDistributed;
		
		// Get the parameters chosen by the user
		System.out.println("Number of classes: " + numClassesTextField.getText());
		System.out.println("Number of models: " + numModelTextField.getText());
		System.out.println("Number of iterations: " + maxIterTextField.getText());
		
		try {
			numClasses = Integer.valueOf(numClassesTextField.getText());
			iterations = Integer.valueOf(numModelTextField.getText());
			maxIteration = Integer.valueOf(maxIterTextField.getText());
		} catch (NumberFormatException e) {
			GUITools.showErrorMessage(parent, "Cannot parse parameters to Integers");
			return;
		}
		
		// Set the total call number of the progress bar
		int totalCalls = iterations * maxIteration;
		pb.setNumberOfTotalCalls(totalCalls);
		
		// Array holding the generated models
		TimeFit[] models = new TimeFit[iterations];
		
		filteredData = new ArrayList<mRNATimeSeries>();
		for(mRNATimeSeries m : data) {
			// Is one value better than the cutoff value?
			for(Signal s : m.getSignals()) {
				// Handle pValues
				if(s.getType() == SignalType.pValue && s.getSignal().doubleValue() < cutoff) {
					filteredData.add(m);
					break;
				}
				// Handle Fold changes
				else if(s.getType() == SignalType.FoldChange && Math.abs(s.getSignal().doubleValue()) >= cutoff) {
					filteredData.add(m);
					break;
				}
			}
		}
		
		// We need at least 3 * numClasses genes.
		if(filteredData.size() / 3 < numClasses) {
			GUITools.showErrorMessage(parent, "Not enough genes remained after filtering.");
			return;
		}
		
		// Generate the models
		for(int i=0; i<iterations; i++) {
			TimeFit tf = new TimeFit(numClasses, maxIteration);
			tf.generateModel(filteredData, timePoints, isExponentiallyDistributed, pb);
			models[i] = tf;
			pb.setCallNr((i+1) * maxIteration);
		}
		
		// Choose the best model. That means the model with the highest logLikelihood.
		TimeFit bestModel = models[0];
		double bestLogLikelihood = models[0].logLikelihood;
		for(int i = 1; i < iterations; i++) {
			if(models[i].logLikelihood > bestLogLikelihood) {
				bestLogLikelihood = models[i].logLikelihood;
				bestModel = models[i];
			}
		}
		
		bestModel.printModel();
		
		// Take the parameter of the best model
		isModelled = true;
		this.q = bestModel.q;
		this.knots = bestModel.knots;
		this.controlPoints = bestModel.controlPoints;
		this.controlPointsColumn = bestModel.controlPointsColumn;
		this.s = bestModel.s;
		this.sts = bestModel.sts;
		this.yMatrix = bestModel.yMatrix;
		this.mu = bestModel.mu;
		this.pos2class = bestModel.pos2class;
		this.class2genes = bestModel.class2genes;
		this.covMatrices = bestModel.covMatrices;
		this.inverseCovMatrices = bestModel.inverseCovMatrices;
		this.gamma = bestModel.gamma;
		this.geneVariances = bestModel.geneVariances;
		this.variance = bestModel.variance;
		this.threshold = bestModel.threshold;
		this.maxIteration = bestModel.maxIteration;
		this.classProbs = bestModel.classProbs;
		this.logLikelihood = bestModel.logLikelihood;
		this.numClasses = bestModel.numClasses;
		this.numGenes = bestModel.numGenes;
		this.x = bestModel.x;
		this.y = bestModel.y;
		this.numDataPoints = bestModel.numDataPoints;	
	}
	
	
	/**
	 * Generate the time series model for the data.
	 * @param data
	 * @param timePoints
	 */
	public void generateModel(ArrayList<mRNATimeSeries> data,
			List<ValueTriplet<Double, String, SignalType>> timePoints,
			boolean isExponentiallyDistributed,
			AbstractProgressBar pb) {
		this.isExponentiallyDistributed = isExponentiallyDistributed;
		
		// Initialize all fields, so that the model can be computed
		filteredData = data;
		// adjust number of classes
		numGenes = data.size();
		if (numGenes / 3 < numClasses) {
			numClasses = numGenes / 3;
			Log.warn("Not enough genes present, reduced number of classes!");
		}
		init(data, timePoints);

		// run the EM-algorithm
		logLikelihood = 0;
		double oldLogLikelihood = 5;

		int iteration = 0;
		while((Math.abs(oldLogLikelihood - logLikelihood) > threshold && iteration < maxIteration) || Double.isInfinite(logLikelihood)) {
			// increase counter
			iteration++;
			pb.DisplayBar();
			
			// Save the current logLikelihood
			oldLogLikelihood = logLikelihood;
			
			/*
			 * E-Step:
			 * 
			 * For all genes i and classes j, compute P(j|i). This is the probability, that gene i
			 * belongs to class j.
			 */ 
			computeProbabilities();
			
			// Finally assign to each gene the corresponding class
			assignClassToGenes();

			/*
			 * M-Step
			 * 
			 * For all genes i and classes j, find the MAP estimate of gamma_ij
			 */
			findMAPEstimate();

			// Compute the new class covariance matrices.
			maximizeCovMatrices();

			// Maximize the covariance matrices Γ, the variance and the class centers mu
			maximizeVariance();

			// Compute the new class centers mu. To do this, we have to compute two matrices and
			// multiplicate them
			maximizeMu();


			// Update the class probabilities
			updateClassProbs();

			// Compute the new log likelihood.
			computeLogLikelihood();	
		}
	}

	/**
	 * Get the model for the mRNATimeSeries. If there is no model (because the gene was filtered out)
	 * return null.
	 * @param dataPoints
	 * @param timePoints
	 * @param gene
	 * @return The model for this mRNATimeSeries. If there is no model (because the gene was filtered out)
	 * null is returned.
	 */
	public TimeFitModel getGeneModel(mRNATimeSeries dataPoints,
			List<ValueTriplet<Double, String, SignalType>> timePoints,
			int gene) {
		TimeFitModel m = new TimeFitModel();
		
		int pos = filteredData.indexOf(dataPoints);
				
		// If the mRNATimeSeries haven't been modelled, return null
		if(pos == -1)
			return null;
		
		// To which class belongs the gene?
		int classOfGene = pos2class[pos];
		
		// Generate the model with the parameters provided by the class of the gene
		m.generateModel(dataPoints, mu.getColumnMatrix(classOfGene), gamma.get(classOfGene).getColumnMatrix(pos),
				controlPoints, knots, timePoints, isExponentiallyDistributed);
		
		return m;
	}


	/* (non-Javadoc)
	 * @see de.zbit.math.TimeSeriesModel#computeValueAtTimePoint(double)
	 */
	@Override
	public double computeValueAtTimePoint(double timePoint, boolean useOriginalData) {
		// Intentially left blank
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.zbit.math.TimeSeriesModel#init(java.util.Collection)
	 */
	@Override
	public void init(List<mRNATimeSeries> data,
			List<ValueTriplet<Double, String, SignalType>> timePoints) {

		// Set the x- and y-values (y-values not needed here, but function sets them)
		processTimePoints(data.get(0), timePoints, isExponentiallyDistributed);
		
		// Generate a matrix from the mRNATimeSeries data
		yMatrix = timeSeries2Matrix(data);
		
		// How many time points and genes do we have?
		numGenes = yMatrix.getColumnDimension();
		numDataPoints = yMatrix.getRowDimension();

		// Place the control points
		chooseControlPoints(x);

		// Place the knots
		placeKnots();

		// Compute the values of the basis B-splines
		// S, where S_[ij] = b_{j,4}(t_i)
		computeS();

		// 1. Initialize the class center for each class as described in the paper.
		// The center of a class is the mean value of the spline coefficients of genes in the class.
		initializeClassCenter();

		// Initialize the assignment of genes to their classes.
		// The classes are initialized
		initializeGenes2ClassMapping();

		// Compute the covariance matrix for each class
		initializeCovMatrices();

		// Initialize the inverse covariance matrices.
		computeInverseCovMatrices();	

		// Sample the gene specific variation coefficients. Each gene has q (number of control points)
		// variation coefficients, which are sampled with the class covariance matrix.
		computeGamma();

		// Initialize the gene variances and sample the noise vector for each gene.
		computeGeneVariances();

		// Sample the noise vector for each gene
		sampleNoiseVectors();

		// Now find the best parameters and class assignment with an EM-algorithm
		initializeClassProbs();

		setInitialized(true);
	}


	/**
	 * The Cox-deBoor recursion formula to calculate the normalized B-Spline basis.
	 * 
	 * @param i number of the basis spline
	 * @param k the order of the basis polynomial (i.e. k=4 for a cubic polynomial)
	 * @param t the argument for the basis spline
	 * @return
	 */
	public double normalizedBSplineBasis(int i, int k, double t) {
		// The base case of the recursion
		if(k == 1) {
			if((knots[i] <= t) && (t < knots[i+1]))
				return 1;
			else
				return 0;
		}

		// The two recursive calls: rec1 = b_{i,k-1}(t), rec2 = b_{i+1,k-1}(t)
		double rec1 = normalizedBSplineBasis(i, k-1, t);
		double rec2 = normalizedBSplineBasis(i+1, k-1, t);

		return ((t-knots[i])*rec1)/(knots[i+k-1]-knots[i]) + ((knots[i+k]-t)*rec2)/(knots[i+k]-knots[i+1]);
	}


	/**
	 * Transcribe the measured data from mRNATimeSeries to a simple Array of doubles.
	 * This is needed, because most of the computation involves multiplying and adding matrices.
	 */
	private RealMatrix timeSeries2Matrix(List<mRNATimeSeries> data) {
		// How many genes and how many experiments do we have?
		int numGenes = data.size();
		int numExperiments = data.get(0).getNumberOfSignals();
		double[][] res = new double[numExperiments][numGenes];

		// Transcribe the given data into the matrix
		for(int i=0; i<numGenes; i++) {
			mRNATimeSeries m = data.get(i);
			List<Signal> signals = m.getSignals();
			for(int j=0; j<numExperiments; j++) {
				res[j][i] = signals.get(j).getSignal().doubleValue();
			}
		}

		return new Array2DRowRealMatrix(res);
	}


	/**
	 * Choose the x-values of the control points used for the model.
	 * The method tries to choose the control points so, that they are roughly equidistant.
	 * Also keep track, to which column of the expression matrix the conrol points belong.
	 * @param timePoints The time points of the available data in ascending order.
	 */
	private void chooseControlPoints(double[] timePoints) {
		// A simple estimation for the number of control points.
		this.q = (timePoints.length / 3)+2;

		// This is the best case we try to achieve: equidistant control points
		double[] equidistantPoints = new double[q]; // Best x-values of the control points
		double distance = x[x.length-1] - x[0];
		equidistantPoints[0] = x[0];
		for(int i=1; i<q-1; i++) {
			equidistantPoints[i] = x[0] + (i * (distance / (q-1)));
		}
		equidistantPoints[q-1] = x[x.length-1];

		// Keep track of the columns of the expression matrix belonging to the control points.
		controlPointsColumn = new int[q];
		controlPointsColumn[0] = 0;

		// Select q control points, so that they are roughly equidistant
		controlPoints = new ArrayList<Point2D>(q);
		controlPoints.add(new Point2D.Double(x[0], 0)); // we're not interested in the y-value yet
		// The first and last of the q control point is given, so choose the q-2 other
		// control points.
		for(int i=1; i<q-1; i++) {
			// Search the given timepoints for the time point with the minimal distance
			// to i-th equidistantPoint
			double oldDistance = Double.MAX_VALUE;
			int lastIndex = 1; // The index of the last timePoint, so we don't choose two similar timePoints
			for(int j=lastIndex; j < timePoints.length-1; j++) {
				distance = Math.abs(timePoints[j] - equidistantPoints[i]);
				if(distance < oldDistance)
					oldDistance = distance; // Found a better timepoint					
				else {
					lastIndex = j;
					controlPoints.add(new Point2D.Double(x[j-1], 0)); // The previous point was better
					controlPointsColumn[i] = j-1; // column of the control point in the expression matrix
					break;
				}
			}
		}
		controlPoints.add(new Point2D.Double(x[x.length-1], 0));
		controlPointsColumn[q-1] = numDataPoints-1;
	}


	/**
	 * 
	 */
	private void computeS() {
		double[][] s_matrix = new double[numDataPoints][q];

		for(int i=0; i<numDataPoints; i++) {
			for(int j=0; j<q; j++) {
				s_matrix[i][j] = normalizedBSplineBasis(j,4,x[i]);
			}
		}
		s = new Array2DRowRealMatrix(s_matrix);

		// S_transposed * S
		sts = s.transpose().multiply(s);
	}


	/**
	 * Place the knots equidistant between the first and last time point.
	 * There are {@link #q} + {@link #order} knots.
	 */
	private void placeKnots() {
		// How many knots have to be placed?
		int numberOfKnots = q + order;
		knots = new double[numberOfKnots];

		// Place the knots. Three knots before the first time point and three knots after the
		// last time point are needed to describe the resulting model in the interval
		double stepsize = (x[x.length-1] - x[0]) / (numberOfKnots-7);
		for(int i=-3; i<numberOfKnots-3; i++) {
			knots[i+3] = x[0] + stepsize * i;
		}
	}


	/**
	 * Initialize the class centers with a random chosen gene.
	 */
	private void initializeClassCenter() {
		this.mu = new Array2DRowRealMatrix(q, numClasses);
		// The already chosen class centers. Don't allow a two same class centers.
		ArrayList<Integer> chosenGenes = new ArrayList<Integer>(numClasses);

		// Initialize the class centers 
		int i;  // number of gene
		for(int j=0; j<numClasses; j++) {
			// Choose a new random gene.
			i = (int) (Math.random() * numGenes);
			while (chosenGenes.contains(i)) {
				i = (int) (Math.random() * numGenes);
			}
			setClassCenter(j, i);
			chosenGenes.add(i);
		}		
	}
	
	
	/**
	 * Set the j-th class center using the i-th gene.
	 */
	private void setClassCenter(int j, int i) {
		RealMatrix m = new SingularValueDecomposition(sts).getSolver().getInverse();
		// Compute the final initial class center, a q by 1 vector
		m = m.multiply(s.transpose()).multiply(yMatrix.getColumnMatrix(i));
		// Add the class center to the list of class centers
		mu.setColumnMatrix(j, m); // the j-th center is in the j-th column of mu
	}

	
	/**
	 * Select a class for each gene at random and create mappings from genes2class and the
	 * other way round.
	 */
	private void initializeGenes2ClassMapping() {
		class2genes = new ArrayList<ArrayList<Integer>>(numClasses);
		for(int i=0; i<numClasses; i++) {
			class2genes.add(new ArrayList<Integer>());
		}
		// Initialize the mapping from a gene to its class
		pos2class = new int[numGenes];

		// For each gene, select a class j uniformly at random.
		for(int i=0; i<numGenes; i++) {
			int j = (int) (Math.random() * numClasses); // Select a random class j for gene i
			pos2class[i] = j;
			// Add the gene to the class2gene mapping
			class2genes.get(j).add(i);
		}
		// make sure that each class contains at least 3 genes
		for(int j=0; j<numClasses; j++) {
			while (class2genes.get(j).size() < 3) {
				for(int c=0; c<numClasses; c++) {
					if (class2genes.get(c).size() > 3) {
						int gene = class2genes.get(c).remove(0);
						class2genes.get(j).add(gene);
						pos2class[gene] = j;
						break;
					}
				}
			}
		}
	}


	/**
	 * Initialize the covariance matrix for each class.
	 */
	private void initializeCovMatrices() {
		// This array is used to index the rows of the expression values for the submatrix
		int[] colsPrimitive;

		covMatrices = new ArrayList<RealMatrix>(numClasses);
		RealMatrix subMatrix;
		for(int j=0; j<numClasses; j++) {
			// Get a submatrix with the expression values of genes of class j
			Integer[] cols = new Integer[class2genes.get(j).size()];
			colsPrimitive = ArrayUtils.toPrimitive(class2genes.get(j).toArray(cols));
			subMatrix = yMatrix.getSubMatrix(controlPointsColumn, colsPrimitive);

			// Compute and set the covariance matrix for class j
			Covariance cov = new Covariance(subMatrix.transpose());
			covMatrices.add(cov.getCovarianceMatrix());
		}
	}
	
	
	/**
	 * Initialize the class probabilities. That is the initial probability that a gene i belongs to class j.
	 */
	private void initializeClassProbs() {
		// At the beginning, each class has the same probability
		double p = 1.0 / numClasses;
		classProbs = new double[numClasses];
		for(int j=0; j<numClasses; j++) {
			classProbs[j] = p;
		}
	}


	/**
	 * Compute the inverse covariance matrices of @link{de.zbit.math.TimeFit.covMatrices}.
	 */
	private void computeInverseCovMatrices() {
		inverseCovMatrices = new ArrayList<RealMatrix>(numClasses);
		for(int j=0; j<numClasses; j++) {
			inverseCovMatrices.add(new SingularValueDecomposition(covMatrices.get(j)).getSolver().getInverse());
		}	
	}


	/**
	 * 
	 */
	private void computeGamma() {
		gamma = new ArrayList<RealMatrix>(numClasses);
		MultivariateNormalDistribution dist;
		// Iterate over the classes and sample matrix of the variation coefficients
		for(int j=0; j<numClasses; j++) {
			// The matrix for the variation coefficients of class j
			RealMatrix m = new Array2DRowRealMatrix(q, numGenes);
			try {
				// The distribution of the variation coefficients for class j
				dist = new MultivariateNormalDistribution(new double[q], covMatrices.get(j).getData());
				// Fill the matrix with sample values
				for(int i=0; i<numGenes; i++) {
					m.setColumn(i, dist.sample());				
				}
			} catch (SingularMatrixException e) {
				Log.warn("Singular covariance matrix!");
			}
			gamma.add(m);
		}
	}


	/**
	 * 
	 */
	private void computeGeneVariances() {
		geneVariances = new double[numGenes];
		for(int i=0; i<numGenes; i++) {
			geneVariances[i] = MathUtils.variance(yMatrix.getColumnMatrix(i).getSubMatrix(controlPointsColumn, new int[1]).getColumn(0)); 
		}
		variance = de.zbit.util.ArrayUtils.sum(geneVariances) / numGenes;
	}


	/**
	 * 
	 */
	private void sampleNoiseVectors() {
		noiseVectors = new Array2DRowRealMatrix(numGenes, q);
		Random rand = new Random();
		for(int i=0; i<numGenes; i++) {
			for(int j=0; j<q; j++) {
				noiseVectors.setEntry(i, j, rand.nextGaussian() * geneVariances[i]);
			}
		}
	}


	/**
	 * 
	 */
	private void computeProbabilities() {
		// For all genes i and classes j, compute P(j|i). This is the probability, that gene i
		// belongs to class j.
		if(probs == null) {
			probs = new double[numGenes][numClasses];			
		}
		for(int i=0; i<numGenes; i++) {
			double[] factors = new double[numClasses];
			double sumFactors = 0;
			double e1;
			double e2;
			factors = new double[numClasses];
			// Compute the factors uses for this gene once
			for(int j = 0; j<numClasses; j++) {
				// The factors
				RealMatrix m1 = yMatrix.getColumnMatrix(i).subtract(s.multiply((mu.getColumnMatrix(j).add(gamma.get(j).getColumnMatrix(i)))));
				e1 = -(m1.transpose().multiply((m1.scalarMultiply(1 / variance))).getEntry(0, 0));
				e2 = -0.5 * (gamma.get(j).getColumnMatrix(i).transpose().multiply(inverseCovMatrices.get(j)).multiply(gamma.get(j).getColumnMatrix(i)).getEntry(0, 0));
				factors[j] = Math.log(classProbs[j]) + e1 + e2;
			}
			
			// Sum up the factors
			sumFactors = NumberUtils.max(factors);

			// Use the computed factors to compute P(j|i)
			for (int j = 0; j<numClasses; j++) {
				probs[i][j] = Math.exp(factors[j] - sumFactors);	
			}
		}
	}


	/**
	 * 
	 */
	private void findMAPEstimate() {
		for(int j=0; j<numClasses; j++) {
			RealMatrix m1;		// a factor for the MAP estimate, computed once for each class
			m1 = inverseCovMatrices.get(j).scalarMultiply(variance).add(sts);
			m1 = new SingularValueDecomposition(m1).getSolver().getInverse().multiply(s.transpose());
			// The gamma matrix of class j
			RealMatrix new_gamma = gamma.get(j);
			// Compute the MAP estimate of gamma_ij for each gene
			for(int i=0; i<numGenes; i++) {
				RealMatrix m2 = (yMatrix.getColumnMatrix(i).subtract(s.multiply(mu.getColumnMatrix(j))));
				new_gamma.setColumnMatrix(i, m1.multiply(m2));
			}
			// Set the new gamma matrix for class j
			gamma.set(j, new_gamma);
		}
	}


	/**
	 * 
	 */
	private void maximizeVariance() {
		// Assume that n_i in the paper is the number of experiments (measured points) for gene i
		// Because here all genes have the same number of experiments
		int n = numGenes * numDataPoints;
		// The traces of some specific matrices are needed. The j-th value represents the trace computed
		// with the use of j-th inverted covariance matrix
		double[] traces = new double[numClasses];
		for(int j=0; j<numClasses; j++) {
			traces[j] = (new SingularValueDecomposition(inverseCovMatrices.get(j).add(sts)).getSolver()).getInverse().add(sts).getTrace();
		}

		// For the variance, we have to compute a sum over genes and classes
		double sum = 0;
		// Two factors used for better coed
		for(int i=0; i<numGenes; i++) {
			for(int j=0; j<numClasses; j++) {
				RealMatrix m;
				m = yMatrix.getColumnMatrix(i).subtract(s.multiply(mu.getColumnMatrix(j).add(gamma.get(j).getColumnMatrix(i))));
				sum += probs[i][j] * m.transpose().multiply(m).getEntry(0, 0);// + traces[j];
			}
		}
		variance = sum / n;
	}


	/**
	 * 
	 */
	private void maximizeMu() {
		for(int j=0; j<numClasses; j++) {
			RealMatrix m1 = new Array2DRowRealMatrix(q, q);
			RealMatrix m2 = new Array2DRowRealMatrix(q, 1);
			// Each gene plays a role for the new class center.
			for(int i=0; i<numGenes; i++) {
				m1 = m1.add(sts.scalarMultiply(probs[i][j]));
				m2 = m2.add(s.transpose().scalarMultiply(probs[i][j]).multiply(yMatrix.getColumnMatrix(i).subtract(s.multiply(gamma.get(j).getColumnMatrix(i)))));
			}
			// m1 has to be inverted
			m1 = new SingularValueDecomposition(m1).getSolver().getInverse();
			mu.setColumnMatrix(j, m1.multiply(m2));
		}
	}


	/**
	 * 
	 */
	private void maximizeCovMatrices() {
		// Compute at first summands that are used very often in the following part.
		ArrayList<RealMatrix> summands = new ArrayList<RealMatrix>(numClasses);
		for(int j=0; j<numClasses; j++) {
			RealMatrix m1;
			m1 = inverseCovMatrices.get(j).add(sts.scalarMultiply(1/variance));
			summands.add(new SingularValueDecomposition(m1).getSolver().getInverse());
		}		
			
		for(int j=0; j<numClasses; j++) {
			RealMatrix numerator = new Array2DRowRealMatrix(q, q);
			double denominator = 0;
			RealMatrix m; // a factor
			// Build sum over the genes
			for(int i=0; i<numGenes; i++) {
				m = gamma.get(j).getColumnMatrix(i).multiply(gamma.get(j).getColumnMatrix(i).transpose());
				numerator = numerator.add(m.add(summands.get(j)).scalarMultiply(probs[i][j]));
				denominator += probs[i][j];
			}
			// The final result
			m = numerator.scalarMultiply(1/denominator);
			covMatrices.set(j, m);
			// Compute also the new inverse of the covMatrix
			inverseCovMatrices.set(j, new LUDecomposition(m).getSolver().getInverse());
		}
	}


	/**
	 * 
	 */
	private void updateClassProbs() {
		for(int j=0; j<numClasses; j++) {
			double sum = 0;
			for(int i=0; i<numGenes; i++) {
				sum += probs[i][j];
			}
			classProbs[j] = sum / numGenes;
		}
	}


	/**
	 * 
	 */
	private void computeLogLikelihood() {
		double newLogLikelihood = 0;
		// Square root of the covMatrix determinants are often needed
		double[] squareRootDets = new double[numClasses];
		for(int j=0; j<numClasses; j++) {
			squareRootDets[j] = Math.sqrt(new LUDecomposition(covMatrices.get(j)).getDeterminant());
		}
		// Now compute the log likelihood
		for(int i=0; i<numGenes; i++) {
			double exp1 = 0; // The first exponent
			double exp2 = 0; // The second exponent
			double sum = 0;
			// This indicator variable (dummy variable) assignes each gene to exactly one class.
			// So this is the class j for gene i with the highest probability.
			int j = findClassOfGene(probs[i]);
			//for(int j=0; j<numClasses; j++) {
			RealMatrix m = yMatrix.getColumnMatrix(i).subtract(s.multiply(mu.getColumnMatrix(j).add(gamma.get(j).getColumnMatrix(i))));
			exp1 = - (m.transpose().multiply(m).scalarMultiply(1/(2*variance)).getEntry(0, 0));
			exp2 = -0.5 * (gamma.get(j).getColumnMatrix(i).transpose().multiply(inverseCovMatrices.get(j)).multiply(gamma.get(j).getColumnMatrix(i)).getEntry(0, 0));
			sum += (1/Math.pow(Math.sqrt(variance), numDataPoints)) * Math.exp(exp1) * 1/squareRootDets[j] * Math.exp(exp2);
			//}
			newLogLikelihood += Math.log(sum);
		}
		logLikelihood = newLogLikelihood;
	}

	
	/**
	 * Assigns the corresponding class to each gene.
	 */
	private void assignClassToGenes() {
		for(int i=0; i<numGenes; i++) {
			int oldClass = pos2class[i];
			int newClass = findClassOfGene(probs[i]);
			pos2class[i] = newClass;
			if(oldClass != newClass) {
				class2genes.get(oldClass).remove(new Integer(i));
				class2genes.get(newClass).add(new Integer(i));
			}
		}
	}
	

	/**
	 * Find the class of the gene. That means the class with the maximum
	 * probability.
	 * @param probs
	 * @return
	 */
	private int findClassOfGene(double[] probs) {
		double max = Double.MIN_VALUE;
		int pos = 0;
		for(int i=0; i<probs.length; i++) {
			if(probs[i] > max) {
				max = probs[i];
				pos = i;
			}
		}
		return pos;
	}

//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {	
//		// Load the test data
//		//ArrayList<mRNATimeSeries> data = loadmRNATimeSeriesData("/home/fex/Dropbox/Paper/MCF-7.txt");
//		String path = "/home/fex/Dropbox/Paper/MCF-7.txt";
//		mRNATimeSeriesReader r = new mRNATimeSeriesReader();
//		ArrayList<mRNATimeSeries> data = (ArrayList<mRNATimeSeries>) r.importWithGUI(new JPanel(), path);
//		
//		
//		// Generate model
//		TimeFit tf = new TimeFit(9, 20);
//		NSTimeSeriesTab parent = new NSTimeSeriesTab(null, null);
//		tf.generateModel(parent, data, r.getTimePoints(), 5, 1.0, false, NSTimeSeriesTab.generateLoadingPanel(parent, "..."));
//		
//		//tf.printModel();
//	}


	/**
	 */
	public void printModel() {
		// Print the classes and the genes in them
		for(int j=0; j<numClasses; j++) {
			System.out.println("Class " + j + "contains " + class2genes.get(j).size() + " genes");
		}
	}

	
	/**
	 * The dialog asking the user for the parameters for the TimeFit model generation.
	 * @return {@link JComponent} where the user can choose parameters
	 */
	@Override
	public JComponent getIndividualSettingsPanel() {
		// The field for the number of models to create
		// Later, among the created models the best model is chosen
		JLabel numModelLabel = new JLabel("Number of models");	
		int numModels = 5;	
		String numModelTooltip = "<html>Number of models that TimeFit will create.<br>"
				+ "Among these models these models the best will be chosen.<br>"
				+ "CAUTION: The higher the number of models, the longer the run time will be.<br></html>";
		DecimalFormat format = new DecimalFormat("##");
		numModelTextField = new JFormattedTextField(format);
		numModelTextField.setText(String.valueOf(numModels));
		numModelTextField.setToolTipText(numModelTooltip);
		// build numModel panel
		JComponent numModelPanel = new JPanel(new GridLayout(1,2));
		numModelPanel.add(numModelLabel);
		numModelPanel.add(numModelTextField);
		
		// The field for the maximum iterations per model
		JLabel maxIterLabel = new JLabel("Maximum iterations per model");	
		// Initial guess of the cutoff value. 
		int maxIter = 50;	
		String maxIterTooltip = "<html>A TimeFit model is computed iteratively computed.<br>"
				+ "Here you can choose the maximum iterations performed per model.<br>"
				+ "CAUTION: The higher the number of iterations, the longer the run time will be.<br></html>";
		maxIterTextField = new JFormattedTextField(format);
		maxIterTextField.setText(String.valueOf(maxIter));
		maxIterTextField.setToolTipText(maxIterTooltip);
		// build numModel panel
		JComponent maxIterPanel = new JPanel(new GridLayout(1,2));
		maxIterPanel.add(maxIterLabel);
		maxIterPanel.add(maxIterTextField);
		
		// The field for the number of classes.
		JLabel numClassesLabel = new JLabel("Number of clusters");	
		// Initial guess of the cutoff value. 
		int numClasses = 9;	
		String numClassesTooltip = "<html>Please choose the number of clusters.<br>"
				+ "CAUTION: The higher the number of clusters, the longer the run time will be.<br></html>";
		numClassesTextField = new JFormattedTextField(format);
		numClassesTextField.setText(String.valueOf(numClasses));
		numClassesTextField.setToolTipText(numClassesTooltip);
		// build numClasses panel
		JComponent numClassesPanel = new JPanel(new GridLayout(1,2));
		numClassesPanel.add(numClassesLabel);
		numClassesPanel.add(numClassesTextField);
		
		// The resulting individual panel
		JComponent panel = new JPanel(new GridLayout(3,1));
		panel.add(numModelPanel);
		panel.add(maxIterPanel);
		panel.add(numClassesPanel);
		numModelPanel = GUITools.createTitledPanel(panel, "TimeFit settings");

		return panel;
	};
	
	
	/**
	 * Was the model generated?
	 */
	public boolean isModelled() {
		return isModelled;
	}

	/**
	 * Return the class of gene i.
	 * @param mRNA The time series of the gene
	 * @param i The gene
	 * @return
	 */
	public int getClassOfGene(mRNATimeSeries mRNA, int i) {
		int pos = filteredData.indexOf(mRNA);
		return pos2class[pos];
	}
}
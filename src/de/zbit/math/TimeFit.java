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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.sbml.layout.y.YMacromolecule;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * Implementation of the TimeFit algorithm to compute a continous representation
 * of time series data.
 * See: Bar-Joseph, Z. et al. (2003).
 * 			Continuous representations of time-series gene expression data.
 * 			Journal of Computational Biology.
 *
 * @see <a href="http://www.ncbi.nlm.nih.gov/pubmed/12935332">The PubMed entry of the paper</a>
 * 
 * @author Felix Bartusch
 * @version $Rev$
 */
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
	 * The result of transpodes s multiplied with s. Because it is used many times. 
	 */
	RealMatrix sts;
	
	/**
	 * The measured data for ALL genes.
	 */
	RealMatrix yMatrix;
	
	/**
	 * The class centers. The center of class 1 is the first ROW and so on.
	 */
	RealMatrix mu;
	
	/**
	 * The mapping of a gene to his class.
	 */
	int[] gene2class;
	
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
	 * The Gene specific variation coefficients. The j-th matrix pf the list contains the  variation
	 * coefficients for the j-th class. The rows of a matrix corresponds to genes and the columns
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
	 * The class probability that are updatet in the EM-algorithm.
	 */
	double[] classProbs;
	
	/**
	 * The log likelihood of the data given the model.
	 */
	double logLikelihood;

	/* (non-Javadoc)
	 * @see de.zbit.math.TimeSeriesModel#generateModel(de.zbit.data.mRNA.mRNATimeSeries, java.util.List)
	 */
	@Override
	public void generateModel(mRNATimeSeries dataPoints,
			List<ValueTriplet<Double, String, SignalType>> timePoints) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.zbit.math.TimeSeriesModel#computeValueAtTimePoint(double)
	 */
	@Override
	public double computeValueAtTimePoint(double timePoint) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see de.zbit.math.TimeSeriesModel#init(java.util.Collection)
	 */
	@Override
	public void init(List<mRNATimeSeries> data,
			List<ValueTriplet<Double, String, SignalType>> timePoints) throws Exception {
		
		this.yMatrix = new Array2DRowRealMatrix(timeSeries2Array(data));
		System.out.println(this.yMatrix.getRowMatrix(0));
		
//		// TODO Write methods (in a new class?) for converting mRNATimeSeries to and ValueTriplets
//		// to Arrays. Arrays can be handled easier than these objects.
//		// TODO Write the JavaDoc
//		// What are the measured time points?
//		numDataPoints = data.get(0).getNumberOfSignals();
//		this.x = new double[numDataPoints];
//		for(int i=0; i<numDataPoints; i++) {
//			x[i] = timePoints.get(i).getA();			// the i-th timePoints
//		}
//				
//		// Check of the data represents a mRNATimeSeries
//		if(data == null)
//			throw new Exception("Data object is null.");
//		else if(data.get(0) instanceof mRNATimeSeries)
//			throw new Exception("Data doesn't represent mRNATimeSeries");
//		
//		// TODO Choose an appropriate number of control points
//		// Choose the number of control points and the control points
//		// chooseControlPoints(data, timePoints);
//		
//		// TODO Implement other knot spacing functions. (chord method ...)
//		// Place the knots
//		placeKnots();
//		
//		int numClasses = 5;
//		ArrayList<mRNATimeSeries> classCenters = new ArrayList<>(numClasses);
//		int numGenes = data.size();
//		int numCols = data.get(0).getNumberOfSignals(); 
//		
//		/*
//		 * The implementation of the TimeFit algorithm
//		 */
//		
//		// Choose a random class center for each class
//		Random rand = new Random();
//		ArrayList<Integer> randInts = new ArrayList<Integer>(5);
//		for(int i=0; i < numClasses; i++){
//			// Choose a random gene as the i-th class center
//			int r = rand.nextInt(numGenes);
//			// Don't allow two equal class centers
//			while(randInts.contains(r))
//				r = rand.nextInt(numGenes);
//						
//			// Set the i-th class Center
//			classCenters.set(i, data.get(r));
//		}
//		
//		
//		
//		setInitialized(true);
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
		// They are computed here, because the resulting formula is more readable.
		double rec1 = normalizedBSplineBasis(i, k-1, t);
		double rec2 = normalizedBSplineBasis(i+1, k-1, t);
		
		return ((t-knots[i])*rec1)/(knots[i+k-1]-knots[i]) + ((knots[i+k]-t)*rec2)/(knots[i+k]-knots[i+1]);
	}
	
	
	/**
	 * Transcribe the measured data from mRNATimeSeries to a simple Array of doubles.
	 * This is needed, because most of the computation involves multiplying and adding matrices.
	 */
	private double[][] timeSeries2Array(List<mRNATimeSeries> data) {
		// How many genes and how many experiments do we have?
		int numGenes = data.size();
		int numExperiments = data.get(0).getNumberOfSignals();
		double[][] res = new double[numGenes][numExperiments];
		
		// Transcribe the given data into the matrix
		for(int i=0; i<numGenes; i++) {
			mRNATimeSeries m = data.get(i);
			List<Signal> signals = m.getSignals();
			for(int j=0; j<numExperiments; j++) {
				res[i][j] = (Double) signals.get(j).getSignal();
			}
		}
		
		return res;
	}
	
	/**
	 * Choose the x-values of the control points used for the model.
	 * The method tries to choose the control points so, that they are roughly equidistant.
	 * Also keep track, to which column of the expression matrix the conrol points belong.
	 * @param timePoints The time points of the available data in ascending order.
	 */
	private void chooseControlPoints(double[] timePoints) {
		// A simple estimation for the number of control points.
		this.q = (timePoints.length / 4)+1;
		
		// This is the best case we try to achieve: equidistant control points
		double[] equidistantPoints = new double[q]; // Best x-values of the control points
		double distance = getLastTimePoint() - getFirstTimePoint();
		equidistantPoints[0] = getFirstTimePoint();
		for(int i=1; i<q-1; i++	) {
			equidistantPoints[i] = i * (getFirstTimePoint() + distance / (q));
		}
		equidistantPoints[q-1] = getLastTimePoint();
		
		// Keep track of the columns of the expression matrix belonging to the control points.
		controlPointsColumn = new int[q];
		controlPointsColumn[0] = 0;
		
		// Select q control points, so that they are roughly equidistant
		controlPoints = new ArrayList<Point2D>(q);
		controlPoints.add(new Point2D.Double(getFirstTimePoint(), 0)); // we're not interested in the y-value yet
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
		controlPoints.add(new Point2D.Double(getLastTimePoint(), 0));
		controlPointsColumn[q-1] = numDataPoints-1;
		
		// for testing
		for(int i=0; i<q; i++) {
			System.out.println("Control Point " + i + "\t opt: " + equidistantPoints[i] + "\t\t\tchosen: " + controlPoints.get(i).getX());
		}
	}
	
	/**
	 * 
	 */
	private Array2DRowRealMatrix computeS() {
		// TODO Write JavaDoc
		double[][] s = new double[numDataPoints][q];
		
		for(int i=0; i<numDataPoints; i++) {
			for(int j=0; j<q; j++) {
				s[i][j] = normalizedBSplineBasis(j,4,i);
			}
		}
		
		Array2DRowRealMatrix res = new Array2DRowRealMatrix(s);
		
		return res;
	}
	
	
	/**
	 * Place the knots equidistant between the first and last time point.
	 * There are {@link #q} + {@link #order} knots.
	 */
	private void placeKnots() {
		// How many knots have to be placed?
		int numberOfKnots = q + order;
		System.out.println(q);
		knots = new double[numberOfKnots];
		
		// Place the knots. Two knots before the first time point and two knots after the
		// last time point are needed to describe the resulting model in the interval
		// [firstTimePoint, lastTimePoint]
		knots[2] = getFirstTimePoint();
		knots[numberOfKnots-3] = getLastTimePoint();
		double stepsize = (getLastTimePoint() - getFirstTimePoint()) / (numberOfKnots-5);
		for(int i=-2; i<numberOfKnots-2; i++)
			knots[i+2] = getFirstTimePoint() + stepsize * i;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Test for a time series with x experiments from timepoint 0 to x
		int numExperiments = 24;
		TimeFit tf = new TimeFit();
		tf.x = new double[numExperiments];
		for(int i=0; i<numExperiments; i++) {
			tf.x[i] = i+1; // start with time point 1
		}
		tf.numDataPoints = 24;
		
		// Generate the test data of 30000 genes. There are 5 classes with a given mean for
		// every time point and a random standard deviation for every time point.
		int numClasses = 5;
		int numGenes = 30000;
		double[][] testData = new double[numGenes][numExperiments];
		double[][]	means = new double[numClasses][numExperiments];
		double[][] sds = new double[numClasses][numExperiments];
		
		// Generate random means and standard deviations. The means are between the
		// given min and max
		Random r = new Random();
		double minMean = -4;
		double maxMean = 4;
		double minSD = .5;
		double maxSD = 1.5;
		for(int i=0; i<numClasses; i++) {
			for(int j=0; j<numExperiments; j++) {
				means[i][j] = minMean + r.nextDouble() * (maxMean-minMean);
				sds[i][j] = minSD + r.nextDouble() * (maxSD-minSD);
			}
		}
		
		// Now sample the test data
		for(int i=0; i<numGenes; i++) {
			// Choose a random class for the gene
			int clas = r.nextInt(numClasses);
			for(int j=0; j<numExperiments; j++){
				testData[i][j] = means[clas][j] + r.nextGaussian() * sds[clas][j];
			}
		}
		
		tf.yMatrix = new Array2DRowRealMatrix(testData); // genes as rows
		
		// Testing output of the first gene.
		System.out.println("The means of the first gene:");
		for(int i=0; i<numExperiments; i++) {
			System.out.print(means[0][i] + "\t");
		}
		System.out.println("\nThe SDs of the first gene:");
		for(int i=0; i<numExperiments; i++) {
			System.out.print(sds[0][i] + "\t");
		}
		System.out.println("\nThe sampled test data of the first gene:");
		for(int i=0; i<numExperiments; i++) {
			System.out.print(testData[0][i] + "\t");
		}
		System.out.println("");
		
		// Test the placing of the control points
		tf.chooseControlPoints(tf.x);
		
		// Test the placing of the knots
		tf.placeKnots();
		for(int i=0; i<tf.knots.length; i++) {
			System.out.println("Knot " + i + ": " + tf.knots[i]);
		}
		
		
		// Compute the values of the basis B-splines. The result is a matrix
		// S, where S_[ij] = b_{j,4}(t_i)
		tf.s = tf.computeS();
		// Testing output
		// System.out.println(tf.s.toString());
		
		
		// 1. Initialize the class center for each class as described in the paper.
		// The center of a class is the mean value of the spline coefficients of genes in
		// the class.
		// The class centers
		tf.mu = new Array2DRowRealMatrix(numClasses, tf.q);
		// The already chosen genes. Don't allow a two same class centers.
		ArrayList<Integer> chosenGenes = new ArrayList<Integer>(numClasses);
		// Initialize the class centers 
		int gene;
		for(int i=0; i<numClasses; i++) {
			// Choose a new random gene.
			gene = (int) (Math.random() * numGenes);
			while (chosenGenes.contains(gene)) {
				gene = (int) (Math.random() * numGenes);
			}
			// Compute the class center
			RealMatrix m = tf.s.transpose().multiply(tf.s); // An intermediary result
			// Invert m
			m = new LUDecomposition(m).getSolver().getInverse();
			// Compute the final initial class center, a q by 1 vector
			m = m.multiply(tf.s.transpose()).multiply(tf.yMatrix.getRowMatrix(gene).transpose());
			// Add the class center to the list of class centers
			tf.mu.setRowMatrix(i, m.transpose()); // the i-th center is in the i-th row
		}
		System.out.println("Initial class centers: " + tf.mu);
		
		
		// Initialize mapping from a class to the set of genes in the class.
		tf.class2genes = new ArrayList<>(numClasses);
		for(int i=0; i<numClasses; i++) {
			tf.class2genes.add(new ArrayList<Integer>());
		}
		// Initialize the mapping from a gene to its class
		tf.gene2class = new int[numGenes];
		
		// For each gene, select a class j uniformly at random.
		int j;
		for(int i=0; i<numGenes; i++) {
			j = (int) (Math.random() * numClasses); // Select a random class j for gene i
			// Assign gene i to class j
			tf.gene2class[i] = j;
			// Add the gene to the class2gene mapping
			tf.class2genes.get(j).add(i);
		}
		
		// Compute the covariance matrix for each class
		// This array is used to index the rows of the expression values for the submatrix
		int[] rowsPrimitive;
		
		tf.covMatrices = new ArrayList<RealMatrix>(numClasses);
		RealMatrix subMatrix;
		System.out.println("Rows: " + tf.yMatrix.getRowDimension() + "Columns: " + tf.yMatrix.getColumnDimension());
		for(j=0; j<numClasses; j++) {
			// Get a submatrix with the expression values of genes of class j
			Integer[] rows = new Integer[tf.class2genes.get(j).size()];
			rowsPrimitive = ArrayUtils.toPrimitive(tf.class2genes.get(j).toArray(rows));
			subMatrix = tf.yMatrix.getSubMatrix(rowsPrimitive, tf.controlPointsColumn);
					
			// Compute and set the covariance matrix for class j
			Covariance cov = new Covariance(subMatrix);
			tf.covMatrices.add(cov.getCovarianceMatrix());
		}
		
		// Initialize the inverse covariance matrices.
		tf.inverseCovMatrices = new ArrayList<RealMatrix>(numClasses);
		for(j=0; j<numClasses; j++) {
			tf.inverseCovMatrices.add(new LUDecomposition(tf.covMatrices.get(j)).getSolver().getInverse());
		}
		
		
		// Sample the gene specific variation coefficients. Each gene has q (number of control points)
		// variation coefficients, which are sampled with the class covariance matrix.
		tf.gamma = new ArrayList<RealMatrix>(numClasses);
		MultivariateNormalDistribution dist;
		// Iterate over the classes and sample matrix of the variation coefficients
		for(j=0; j<numClasses; j++) {
			// The distribution of the variation coefficients for class j
			dist = new MultivariateNormalDistribution(new double[tf.q], tf.covMatrices.get(j).getData());
			// The matrix for the variation coefficients of class j
			RealMatrix m = new Array2DRowRealMatrix(numGenes, tf.q);
			// Fill the matrix with sample values
			for(int i=0; i<numGenes; i++) {
				m.setRow(i, dist.sample());				
			}
			tf.gamma.add(m);
		}
		System.out.println("Gamma : \t" + tf.gamma.get(0).getRowMatrix(1).toString()); // Testing output

		
		// Initialize the gene variances and sample the noise vector for each gene.
		tf.geneVariances = new double[numGenes];
		for(int i=0; i<numGenes; i++) {
			tf.geneVariances[i] = MathUtils.variance(tf.yMatrix.getRowMatrix(i).getSubMatrix(new int[1], tf.controlPointsColumn).getRow(0)); 
		}
		System.out.println("Variance: \t" + tf.geneVariances[1]);
		// Sample the noise vector for each gene
		tf.noiseVectors = new Array2DRowRealMatrix(numGenes, tf.q);
		Random rand = new Random();
		for(int i=0; i<numGenes; i++) {
			for(j=0; j<tf.q; j++) {
				tf.noiseVectors.setEntry(i, j, rand.nextGaussian() * tf.geneVariances[i]);
			}
		}
		System.out.println("Noise vector: \t" + tf.noiseVectors.getRowMatrix(1).toString()); // Testing output

		
		// Now find the best parameters and class assignment with an EM-algorithm
		// At the beginning, each class has the same probability
		double p = 1 / numClasses;
		tf.classProbs = new double[numClasses];
		for(int i=0; i<numClasses; i++) {
			tf.classProbs[i] = p;
		}
		
		
		// TODO Repeat until the log likelihood converges
		// TODO Check if all math things are correct

		/*
		 * E-Step:
		 */
		// For all genes i and classes j, compute P(j|i). This is the probability, that gene i
		// belongs to class j.
		tf.probs = new double[numGenes][numClasses];
		// Factors, which are used numClasses+1 times, so compute them just once.
		double[] factors = new double[numClasses];
		for(int i=0; i<numGenes; i++) {
			// Compute the factors uses for this gene once
			for(j = 0; j<numClasses; j++) {
				// The factors
				RealMatrix m1 = tf.yMatrix.getRowMatrix(i).transpose().subtract(tf.s.multiply((tf.mu.getRowMatrix(j).add(tf.gamma.get(j).getRowMatrix(i)).transpose())));
				double e1 = m1.transpose().multiply(m1).getEntry(0, 0) / tf.variance;
				double e2 = -0.5 * (tf.gamma.get(j).getRowMatrix(i).multiply(tf.inverseCovMatrices.get(j).multiply(tf.gamma.get(j).getRowMatrix(i).transpose())).getEntry(0, 0));
				factors[j] = tf.classProbs[j] * Math.exp(e1) * Math.exp(e2);
			}
			double sumFactors = de.zbit.util.ArrayUtils.sum(factors);
			// Use the computed factors to compute P(j|i)
			for(j = 0; j<numClasses; j++) {
				tf.probs[i][j] = factors[j] / sumFactors;
			}	
		}
		
		
		/*
		 * M-Step
		 * For all genes i and classes j, find the MAP estimate of gamma_ij
		 */
		RealMatrix m1;		// a factor for the MAP estimate, computed once for each class
		for(j=0; j<numClasses; j++) {
			m1 = tf.inverseCovMatrices.get(j).scalarMultiply(tf.variance).add((tf.s.transpose().multiply(tf.s)));
			m1 = new LUDecomposition(m1).getSolver().getInverse();
			// The gamma matrix of class j
			RealMatrix gamma = tf.gamma.get(j);
			// Compute the MAP estimate of gamma_ij for each gene
			for(int i=0; i<numGenes; i++) {
				RealMatrix m2 = tf.s.transpose().multiply((tf.yMatrix.getRowMatrix(i).transpose().subtract(tf.s.multiply(tf.mu.getRowMatrix(j).transpose()))));
				gamma.setRowMatrix(i, m1.multiply(m2).transpose());
			}
		}
		
		// Maximize the covariance matrices Γ, the variance and the class centers mu
		// Assume that n_i in the paper is the number of experiments (measured points) for gene i
		// Because here all genes have the same numbe of experiments
		int n = numGenes * numExperiments;
		tf.sts = tf.s.transpose().multiply(tf.s);
		// The traces of some specific matrices are needed. The j-th value represents the trace computed
		// with the use of j-th inverted covariance matrix
		double[] traces = new double[numClasses];
		for(j=0; j<numClasses; j++) {
			traces[j] = new LUDecomposition(tf.inverseCovMatrices.get(j).add(tf.sts)).getSolver().getInverse().add(tf.sts).getTrace();
		}
		
		// For the variance, we have to compute a sum over genes and classes
		double sum = 0;
		// Two factors used for better coed
		RealMatrix f1;
		RealMatrix f2;
		for(int i=0; i<numGenes; i++) {
			for(j=0; j<numClasses; j++) {
				f1 = tf.yMatrix.getRowMatrix(i).transpose().subtract(tf.s.multiply(tf.mu.getRowMatrix(j).transpose().add(tf.gamma.get(j).getRowMatrix(i).transpose()))).transpose();
				f2 = tf.yMatrix.getRowMatrix(i).transpose().subtract(tf.s.multiply((tf.mu.getRowMatrix(j).transpose().add(tf.gamma.get(j).getRowMatrix(i).transpose().scalarAdd(traces[j])))));
				sum += tf.probs[i][j] * (f1.multiply(f2).getEntry(0, 0));
			}
		}
		tf.variance = sum / n;
		
		// Compute the new class centers mu. To do this, we have to compute two matrices and
		// multiplicate them
		m1 = new Array2DRowRealMatrix(tf.q, tf.q);
		RealMatrix m2 = new Array2DRowRealMatrix(tf.q, 1);
		for(j=0; j<numClasses; j++) {
			// Each gene plays a role for the new class center.
			for(int i=0; i<numGenes; i++) {
				m1 = m1.add(tf.sts.scalarMultiply(tf.probs[i][j]));
				m2 = m2.add(tf.s.transpose().scalarMultiply(tf.probs[i][j]).multiply(tf.yMatrix.getRowMatrix(i).transpose().subtract(tf.s.multiply(tf.gamma.get(j).getRowMatrix(i).transpose()))));
			}
			// m1 has to be inverted
			m1 = new LUDecomposition(m1).getSolver().getInverse();
			tf.mu.setRowMatrix(j, m1.multiply(m2).transpose());
		}
		
		// Compute the new class variance matrices.
		// Compute at first summands that are used very often in the following part.
		ArrayList<RealMatrix> summands = new ArrayList<RealMatrix>(numClasses);
		for(j=0; j<numClasses; j++) {
			m1 = tf.inverseCovMatrices.get(j).add(tf.sts.scalarMultiply(1/tf.variance));
			summands.add(new LUDecomposition(m1).getSolver().getInverse());
		}		
		// Now compute the new covariance matrices
		RealMatrix numerator = new Array2DRowRealMatrix(tf.q, tf.q);
		double denominator = 0;
		RealMatrix m; // a factor
		for(j=0; j<numClasses; j++) {
			// Build sum over the genes
			for(int i=0; i<numGenes; i++) {
				m = tf.gamma.get(j).getRowMatrix(i).transpose().multiply(tf.gamma.get(j).getRowMatrix(i));
				numerator = numerator.add(m.add(summands.get(j)).scalarMultiply(tf.probs[i][j]));
				denominator += tf.probs[i][j];
			}
			// The final result
			m = numerator.scalarMultiply(1/denominator);
			tf.covMatrices.set(j, m);
			// Compute also the new inverse of the covMatrix
			tf.inverseCovMatrices.set(j, new LUDecomposition(m).getSolver().getInverse());
		}
		
		
		// Update the class probabilities
		sum = 0;
		for(j=0; j<numClasses; j++) {
			sum = 0;
			for(int i=0; i<numGenes; i++) {
				sum += tf.probs[i][j];
			}
			tf.classProbs[j] = sum / numGenes;
		}
		
		// Compute the new log likelihood.
		double newLogLikelihood = 0;
		double exp1 = 0; // The first exponent
		double exp2 = 0; // The second exponent
		// Square root of the covMatrix determinants are often needed
		double[] SquareRootDets = new double[numClasses];
		for(j=0; j<numClasses; j++) {
			SquareRootDets[j] = Math.sqrt(new LUDecomposition(tf.covMatrices.get(j)).getDeterminant());
		}
		// Now compute the log likelihood
		for(int i=0; i<numGenes; i++) {
			sum = 0;
			for(j=0; j<numClasses; j++) {
				m = tf.yMatrix.getRowMatrix(i).transpose().subtract(tf.s.multiply(tf.mu.getRowMatrix(j).transpose().add(tf.gamma.get(j).getRowMatrix(i).transpose())));
				exp1 = - (m.transpose().multiply(m).scalarMultiply(1/(2*tf.variance)).getEntry(0, 0));
				exp2 = -0.5 * (tf.gamma.get(j).getRowMatrix(i).multiply(tf.inverseCovMatrices.get(j)).multiply(tf.gamma.get(j).getRowMatrix(i).transpose()).getEntry(0, 0));
				sum += tf.probs[i][j] * (1/Math.pow(tf.variance, numExperiments)) * Math.exp(exp1) * Math.exp(exp2);
			}
			newLogLikelihood += Math.log(sum);
		}

	}
}

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
package de.zbit.io;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.gui.table.JTableRowBased;
import de.zbit.integrator.ReaderCache;
import de.zbit.integrator.ReaderCacheElement;
import de.zbit.io.csv.CSVReader;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * A special reader for the import of {@link Collections} of {@link mRNATimeSeries} and the
 * corresponding time information for each experiment/treatment.
 * @author Felix Bartusch
 * @version $Rev$
 */
public class mRNATimeSeriesReader extends AbstractGeneBasedNSreader<mRNATimeSeries> {
	private static final long serialVersionUID = -8467426139583946335L;

	/**
	 *  String describing the timeUnit, e.g. "sec", "day", "hour", "mol", etc.
	 */
	private String timeUnit = "";

	/**
	 * This list holds the time information.
	 */
	private List<ValueTriplet<Double, String, SignalType>> timePoints;

	/**
	 * This Array holds the experiment names. They are read from the input file
	 */
	private String[] colNames;

	/**
	 * This table holds the time points user input. It is later parsed to a
	 * {@link List<ValueTriplet<String, SignalType, Double>>}.
	 */
	private JTable timePointsTable;

	/**
	 * Number of signal columns of the file
	 */
	private int numSignalColumns;

	public String getTimeUnit() {
		return timeUnit;
	}

	public List<ValueTriplet<Double, String, SignalType>> getTimePoints() {
		return timePoints;
	}

	/* (non-Javadoc)
	 * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String, de.zbit.integrator.ReaderCache)
	 */
	@Override
	public Collection<mRNATimeSeries> importWithGUI(Component parent, String file, ReaderCache cache) {
		// Create a new panel that allows selection of species
		JLabeledComponent spec = IntegratorUITools.getOrganismSelector();
		JPanel timeUnitTextfield = null;

		// Create and show the import dialog
		try {
			// Peak into file to determine maximum number of observations and SignalTypes
			String[][] sampleData = getSampleData(file,5);
			Integer[] isSignalColumn = inferSignalColumns(sampleData);
			boolean isPValue = inferIsPValue(sampleData, isSignalColumn);

			// Definitions of required and optional columns. Signal columns are created in the method
			// getExpectedSignalColumnsOverridable, which is overridden in this class
			ExpectedColumn[] exCol = getExpectedColumns();

			int originalSize = getExpectedColumns().length;
			
			CSVReader inputReader = loadConfigurationFromCache(cache, file, exCol, spec);
			
			// Check if the inputReader has a header
			if(inputReader.getHeader() == null) {
				inputReader.open();
			}
			if (exCol.length!=originalSize) {
				// Don't load corrupt information
				exCol = getExpectedColumns();
			}

			// Show the CSV Import dialog
			CSVImporterV2 c = null;
			boolean dialogConfirmed = true;
			boolean manuallyCheckedAssignments = false;
			while (dialogConfirmed && !manuallyCheckedAssignments) {
				c = new CSVImporterV2(inputReader, exCol);

				// Presumption: Every column, which contains no identifier, but floats is a signal column
				// Get the exColSelections. 1 indicates an identifier column, 0 indicates a not yet specified column
				Integer[] types = c.getExColTypeSelections();

				// Change exColSelection and exColTypeSelection of the CSVImporterV2
				changeExCol(c, isSignalColumn, types, isPValue);

				// Create new panel, that allows to select timeUnit, timePoints and species
				JPanel comp = new JPanel(new BorderLayout());
				timeUnitTextfield = getTimeUnitTextfield();
				// for testing
				timePointsTable = createTimePointTable(inputReader, isSignalColumn);

				// Add single components to the main component
				comp.add(spec, BorderLayout.PAGE_START);
				comp.add(timeUnitTextfield, BorderLayout.CENTER);
				comp.add(createTimePointPanel(timePointsTable), BorderLayout.PAGE_END);

				dialogConfirmed = IntegratorUITools.showCSVImportDialog(parent, c, comp, 800, 600);
				manuallyCheckedAssignments = dialogConfirmed && additionalColumnAssignmentCheck(c);
			}

			// Process user input and read data
			if (dialogConfirmed) {
				// Store in cache
				this.species = (Species) spec.getSelectedItem();
				if (cache!=null) cache.add(ReaderCacheElement.createInstance(c, species));

				// Read all columns and types
				setNameAndIdentifierTypes(exCol[0]);   

				parseCustomAnnotationColumn(exCol[1]);

				int offset = 2;
				List<ExpectedColumn> additional = getAdditionalExpectedColumns(); // Just the size is required
				if (additional!=null && additional.size()>0) {
					processAdditionalExpectedColumns(ArrayUtils.asList(exCol, offset, (offset+additional.size()) ));
					offset+=additional.size();
				}

				// Signal columns (assumes all leftover columns are signal columns!)
				for (int i=offset; i<exCol.length; i++) {
					if (exCol[i].hasAssignedColumns()) {
						for (int j=0; j<exCol[i].getAssignedColumns().size(); j++) {
							addSignalColumn(exCol[i].getAssignedColumns().get(j), 
									(SignalType) exCol[i].getAssignedType(j), exCol[i].getName().toString());
						}
					}
				}

				try {
					// Store time unit and time points from user input
					this.timeUnit = ((JTextField) timeUnitTextfield.getComponents()[1]).getText();
					createTimePointsFromUserInput(c, timePointsTable, exCol); // does nothing
					//Utils.saveObject(FileTools.removeFileExtension(c.getApprovedCSVReader().getFilename())+"-reader.dat", this);
					return read(c.getApprovedCSVReader());

				} catch (Exception e) {
					if(e.getMessage() != null)
						GUITools.showErrorMessage(parent, e, e.getMessage());

					GUITools.showErrorMessage(parent, e, "Could not read input file.");
				}
			}

		} catch (IOException e) {
			GUITools.showErrorMessage(parent, e);
		}

		// Only errors or canceled
		return null;
	}

	/**
	 * 
	 * @param sampleData of the input file
	 * @param isSignalColumn contains 1, if corresponding column contains signal
	 * @return true if data represent p-values, false otherwise
	 */
	private boolean inferIsPValue(String[][] sampleData, Integer[] isSignalColumn) {

		for(int col=0; col<sampleData[0].length; col++) {
			// Is the column a signal column?
			if(isSignalColumn[col] == 1) {
				// check all values, if the are in [0 ,1]
				for(int row=0; row<sampleData.length; row++) {
					Double d = Double.parseDouble(sampleData[row][col]);
					if(!(d >= 0 && d <= 1)) {
						return false;
					}
				}
			}
		}

		return true;
	}


	/**
	 * Infer the number of signal columns in the input file. Each column with Floats is considered as signal column.
	 * @param sampleData First lines of the input file
	 */
	private Integer[] inferSignalColumns(String[][] sampleData) {
		int counter = 0;
		Integer[] ret = new Integer[sampleData[0].length];

		// Count the columns with floats
		for(int col=0; col<sampleData[0].length; col++) {
			boolean isSignalColumn = true;
			boolean isIdentifier = true;												// Some gene identifier are ints

			// Check all values in the current column
			for(int row=0; row<sampleData.length; row++) {
				if(sampleData[row][col] != null) {
					try {
						Double f = Double.parseDouble(sampleData[row][col]);
						if(f % 1 != 0) 																// Number has remainder -> no gene identifier
							isIdentifier = false;
					} catch (Exception e) {
						break;
					}	
				}
			}

			// If current column is a signal column, increase counter and set flag in the array
			if(isSignalColumn && !isIdentifier) {
				counter++;
				ret[col] = 1;
			} else {
				ret[col] = 0;
			}
		}

		numSignalColumns = counter;

		return ret;
	}

	/**
	 * Return sample content from the file
	 * @param numDataLines number of lines to read from the file
	 * @return a uniform 2D array with sample content
	 * @throws IOException
	 */
	private String[][] getSampleData(String file, int numDataLines) throws IOException {
		CSVReader r = new CSVReader(file);
		// Auto infer the seperator char
		r.setSeparatorChar('\u0000');
		// the header isn't interesting, but this calls the private initialize() method
		r.getContainsHeaders();

		// Get Data
		ArrayList<String[]> firstLines = new ArrayList<String[]>(numDataLines);
		int maxColCount=0;
		r.open();

		String[] line;
		int i=0;
		while((line=r.getNextLine())!=null) {
			firstLines.add(line);
			maxColCount = Math.max(maxColCount, line.length);
			if ((++i)==numDataLines) break;
		}
		r.close();
		// ---
		String[][] data = firstLines.toArray(new String[0][0]);

		// Bring them all to the same column count
		for (i=0; i<data.length; i++) {
			if (data[i]!=null && data[i].length<maxColCount) {
				String[] newData = new String[maxColCount];
				System.arraycopy(data[i], 0, newData, 0, data[i].length);
				data[i] = newData;
			}
		}
		return data;
	}

	/**
	 * Update exCols, exColSelections and exColTypeSelection of the CSVImporterV2.
	 * @param c The CSVImporterV2
	 * @param signals which columns contain signals
	 * @param oldTypes  
	 * @param isPValue 
	 * @return 
	 */
	private void changeExCol(CSVImporterV2 c, Integer[] signals, Integer[] oldTypes, boolean isPValue) { 	 
		// Get original exColSelection
		Integer[] exColSelection = c.getExColSelections();

		//Get the preview panel and the preview table
		JComponent previewPanel = c.getPreviewPanel();
		JTableRowBased table = null;
		for(Component com : previewPanel.getComponents()) {
			if(com instanceof JScrollPane) {
				JScrollPane p = (JScrollPane) com;
				table = (JTableRowBased) p.getViewport().getView();
			}
		}

		// Get the tableModel and measures of the preview table
		TableModel model = table.getModel();
		int numCol = model.getColumnCount();

		// Get the ComboBoxes (first two lines of the preview table)
		JComboBox<?>[][] boxes = new JComboBox[2][numCol];

		// Set the Observation for ComboBoxes of signal columns
		int observation = 0;	// counts the current observations
		for(int i=0; i<signals.length; i++) {
			// Get column selection combo boxes. 
			boxes[0][i] = (JComboBox<?>) model.getValueAt(0, i);
			boxes[1][i] = (JComboBox<?>) model.getValueAt(1, i);
			int type = boxes[1][i].getSelectedIndex();

			if(exColSelection[i] != 0 && signals[i] != 1) {
				// This column could be an identifier
				boxes[0][i].setSelectedIndex(exColSelection[i]);
				boxes[1][i].setSelectedIndex(type);
			}else if(signals[i] == 1) {
				// the column is a signal column
				boxes[0][i].setSelectedIndex(observation+3);
				observation++;
			}
		}

		//update the exColTypeSelection array of CSVImporterV2
		Integer[] newExColTypeSelection = c.getExColTypeSelections();

		// Set exColTypeSelection boxes for the observations
		for(int col=0; col<signals.length; col++) {
			// Is the column an observation?
			if(boxes[0][col].getSelectedIndex() > 2) {
				boxes[1][col].setSelectedIndex(isPValue ? 1:0);
				newExColTypeSelection[col] = isPValue ? 1:0;
			}
		}

		c.setExColTypeSelections(newExColTypeSelection);
	}


	/* (non-Javadoc)
	 * @see de.zbit.ioAbstractGeneBasedNSReader.#getExpectedSignalColumnsOverridable(int)
	 */
	@Override
	protected Collection<ExpectedColumn> getExpectedSignalColumnsOverridable(int maxNumberOfObservations) {
		// Application can (as of today) only process pValues and fold changes.
		SignalType[] types = new SignalType[]{SignalType.FoldChange, SignalType.pValue};

		Collection<ExpectedColumn> r = new ArrayList<ExpectedColumn>(maxNumberOfObservations); 
		for (int i=1; i<=numSignalColumns; i++) {
			ExpectedColumn e = new ExpectedColumn("Observation " + i, types, false, false,false,true);
			r.add(e);
		}
		return r;
	}

	/**
	 * User's time points input is stored as two-dimensional array. This is ugly, so create a list from the array.
	 * Ignore the empty cells, because user doesn't want to use this columns.
	 * @param table 
	 * @param offset 
	 * @param exCol 
	 * @throws Exception 
	 * 
	 */
	private void createTimePointsFromUserInput(CSVImporterV2 c, JTable table, ExpectedColumn[] exCol) throws Exception {
		//Get the preview panel and the preview table
		JComponent previewPanel = c.getPreviewPanel();
		JTableRowBased previewTable = null;
		for(Component com : previewPanel.getComponents()) {
			if(com instanceof JScrollPane) {
				JScrollPane p = (JScrollPane) com;
				previewTable = (JTableRowBased) p.getViewport().getView();
			}
		}

		// Get the tableModel and measures of the preview table
		TableModel previewModel = previewTable.getModel();
		int numCol = previewModel.getColumnCount();

		// Get the ComboBoxes (first two lines of the preview table)
		JComboBox<?>[] boxes = new JComboBox[numCol];

		// Set the Observation for ComboBoxes of signal columns
		for(int i=0; i<numCol; i++) {
			// Get column selection combo boxes. 
			boxes[i] = (JComboBox<?>) previewModel.getValueAt(0, i);
		}

		// Read the user input
		this.timePoints = new ArrayList<ValueTriplet<Double, String, SignalType>>();
		TableModel model = table.getModel();

		for(int i=0; i<numCol; i++) {			
			if(boxes[i].getSelectedIndex() > 2) {					// a signal column
				JComboBox<?> typeBox = (JComboBox<?>) previewModel.getValueAt(1, i);

				timePoints.add(
						new ValueTriplet<Double, String, SignalType>(
								(Double) model.getValueAt(0, boxes[i].getSelectedIndex()-3),
								boxes[i].getSelectedItem().toString(),
								(SignalType) typeBox.getSelectedItem()));	
			}
		}

		// sort the time points
		Collections.sort(timePoints);
		for(int i=0; i<timePoints.size(); i++) {
			// Check, that for every selected column a time is given
			if(timePoints.get(i).getA() == null)
				throw new Exception("Please specify time for each selected observation");
			// Check, that now two times are the same
			if(i+1<timePoints.size() && Math.abs((timePoints.get(i).getA() - timePoints.get(i+1).getA())) < 0.000000001)
				throw new Exception("Time points have to be different");
		}
	}

	/**
	 * Show a textfield to let user fill in the time unit
	 * @return
	 */
	private static JPanel getTimeUnitTextfield() {
		JPanel panel = new JPanel();
		// The two components of the TimeUnitTextField
		JTextField field = new JTextField(10); // or a combo box, which is easier to generate and looks better?
		field.setToolTipText("e.g. 'd' for days");		
		JLabel description = new JLabel("Please select your unit");
		description.setHorizontalAlignment(JLabel.RIGHT);		

		// Set Layout and add components
		panel.setLayout(new FlowLayout());
		panel.setPreferredSize(null);
		panel.add(description);
		panel.add(field);

		GUITools.createTitledPanel(panel, "Time unit selection");
		return panel;
	}

	/**
	 * A {@link JTable} with experiment names as column headers. In one single row the user can fill in {@link Double}s,
	 * which describe the time points of the experiment.
	 * @param reader The CSVReader
	 * @param signals The array contains information about signal columns
	 * @return
	 */
	private JTable createTimePointTable(final CSVReader reader, Integer[] signals) {

		// How many columns the table need?
		int counter = 0;
		for(int i=0; i<signals.length; i++) {
			if(signals[i]==1)
				counter++;
		}
		final int numCols = counter;

		// Lookup the column names of the signal columns.
		colNames = new String[numCols];
		String[] header = reader.getHeader();
		// for testing
		int pos = 0;
		for(int i=0; i<signals.length; i++) {
			if(signals[i]==1) {
				colNames[pos] = header[i];
				pos++;
			}
		}

		// create TableModel for the new JTable
		TableModel m = new AbstractTableModel() {
			private static final long serialVersionUID = -5029024007512394484L;
			private String[] columnNames = colNames;
			private Object[][] data = new Double[1][numCols];


			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return data[rowIndex][columnIndex];
			}

			@Override
			public int getRowCount() {
				return data.length;
			}

			@Override
			public int getColumnCount() {
				return columnNames.length;
			}

			@Override
			public String getColumnName(int col) {
				return columnNames[col];
			}

			// Every cell is editable. User has to enter values in every cell.
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return true;	
			}

			// If the input isn't parseable to double, just overwrite the last double in the cell
			@Override
			public void setValueAt(Object val, int row, int column ) {
				try{
					data[row][column] = new Double((String) val);
				} catch(Exception e) {	
					data[row][column] = null;
				}
			}
		};

		// Build new JTable with the given dimension and experiment names
		JTable table = new JTable(m);

		// Edit each cell with just a single click, instead of a double click and add tool tips
		DefaultCellEditor singleclick = new DefaultCellEditor(new JTextField());
		singleclick.setClickCountToStart(1);

		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setToolTipText("Please fill in the corresponding time for each experiment. Leave the cell empty, if an experiment shouldn't be considered.");

		// Add the changes to each column (e.g. cell)
		for(int i=0; i<table.getColumnModel().getColumnCount(); i++) {
			TableColumn col = table.getColumnModel().getColumn(i);
			col.setPreferredWidth(150);
			col.setCellEditor(singleclick);
			col.setCellRenderer(renderer);
		}

		// Don't select the whole row by clicking on a single cell
		table.setRowSelectionAllowed(false);

		// Focusing another component than the table results editing stop
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		// No auto-resizing width
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// While writing into a cell, the lower half of the text isn't visible
		// So increase the RowHeight of the table
		FontMetrics metrics = table.getFontMetrics(table.getFont()); 
		int fontHeight = metrics.getHeight();
		table.setRowHeight(fontHeight + 7);		

		return table;
	}


	private JScrollPane createTimePointPanel(JTable table) {
		// Add a horizontal scrollpane to the table
		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(200, 95)); //100
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		scrollPane.getViewport().add(table, BorderLayout.NORTH);

		GUITools.createTitledPanel(scrollPane, "Set time points");
		return scrollPane;
	}

	@Override
	protected List<ExpectedColumn> getAdditionalExpectedColumns() {
		return null;
	}

	@Override
	protected void processAdditionalExpectedColumns(
			List<ExpectedColumn> additional) {		
	}

	/* (non-Javadoc)
	 * @see de.zbit.io.AbstractGeneBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.String[])
	 */
	@Override
	protected mRNATimeSeries createObject(String name, Integer geneID, String[] line) {
		// Create mRNA
		mRNATimeSeries m;
		if (geneID!=null) {
			m = new mRNATimeSeries(name, geneID);
		} else {
			m = new mRNATimeSeries(name);
		}

		// XXX: We, until today, support no probe-operations on mRNAs. => Remove probe annotation.
		m.unsetProbeName();

		return m;
	}
}

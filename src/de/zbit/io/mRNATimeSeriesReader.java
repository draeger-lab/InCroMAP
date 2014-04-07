package de.zbit.io;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.treetable.AbstractCellEditor;

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

public class mRNATimeSeriesReader extends AbstractGeneBasedNSreader<mRNA> {

	/**
	 *  String describing the timeUnit, e.g. "sec", "day", "hour", "mol", etc.
	 */
	private String timeUnit = "";
	
	/**
	 * This Array holds the time information. The first entry belongs to the first SignalColumn
	 * and so on. There must be as many timePoints as SignalColumns.
	 * An Array, so that a JTable can use this field storing the user input.
	 */
	private List<Float> timePoints;
	
	/**
	 * This Array holds the experiment names. They are read from the input file
	 */
	private String[] colNames;
	
	/**
	 * This Array holds the time points user input. It is later parsed to a {@link List<Float>}.
	 */
	private JTable timePointsTable;
	
	
	/* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String, de.zbit.integrator.ReaderCache)
   */
  @Override
  public Collection<mRNA> importWithGUI(Component parent, String file, ReaderCache cache) {

  	// Create new panel, that allows to select timeUnit, timePoints and species
  	JPanel comp = new JPanel();
  	comp.setLayout(new GridLayout(3, 1));
  	
    // Create a new panel that allows selection of species
    JLabeledComponent spec = IntegratorUITools.getOrganismSelector();
    
    // Create a JTextField that allows specification of the timeUnit
    JPanel timeUnit = getTimeUnitTextfield();
    
    // Add single components to the main component
    comp.add(spec);
    comp.add(timeUnit);
    
    // Create and show the import dialog
    try {
      // Definitions of required and optional columns
      ExpectedColumn[] exCol = getExpectedColumns();
      int originalSize = getExpectedColumns().length;
      CSVReader inputReader = loadConfigurationFromCache(cache, file, exCol, spec);
      if (exCol.length!=originalSize) {
        // Don't load corrupt information
        exCol = getExpectedColumns();
      }
      // getExpectedSignalColumnsOverridable ueberschreiben?
      
      // Show the CSV Import dialog
      CSVImporterV2 c = null;
      boolean dialogConfirmed = true;
      boolean manuallyCheckedAssignments = false;
      while (dialogConfirmed && !manuallyCheckedAssignments) {
        c = new CSVImporterV2(inputReader, exCol);
               
        // Create new panel, that allows specification of time points
        timePointsTable = createTimePointTable(inputReader);
        comp.add(createTimePointPanel(timePointsTable));

        dialogConfirmed = IntegratorUITools.showCSVImportDialog(parent, c, comp);
        manuallyCheckedAssignments = dialogConfirmed && additionalColumnAssignmentCheck(c);
      }
      
      // Process user input and read data
      if (dialogConfirmed) {
        // Store in cache
        this.species = (Species) spec.getSelectedItem();
        if (cache!=null) cache.add(ReaderCacheElement.createInstance(c, species));
        
        // Store time unit and time points from user input
        this.timeUnit = ((JTextField) timeUnit.getComponents()[1]).getText();
        createTimePointsFromUserInput(timePointsTable);

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
          //Utils.saveObject(FileTools.removeFileExtension(c.getApprovedCSVReader().getFilename())+"-reader.dat", this);
          return read(c.getApprovedCSVReader());
        } catch (Exception e) {
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
   * User's time points input is stored as two-dimensional array. This is ugly, so create a list from the array.
   * Ignore the empty cells, because user doesn't want to use this columns.
   * @param table 
   * 
   */
	private void createTimePointsFromUserInput(JTable table) {
		this.timePoints = new ArrayList<Float>();
		
		TableModel m = table.getModel();

		for(int i=0; i<m.getColumnCount(); i++) {
			if(m.getValueAt(0, i) != null) 
				this.timePoints.add((Float) m.getValueAt(0, i));
		}
	}
	
	public String getTimeUnit() {
		return timeUnit;
	}

	public List<Float> getTimePoints() {
		return timePoints;
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
		JLabel description = new JLabel("Please select your time unit");
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
	 * A {@link JTable} with experiment names as column headers. In one single row the user can fill in {@link Floats},
	 * which describe the time points of the experiment.
	 * @param reader The CSVReader
	 * @return
	 */
	private JTable createTimePointTable(final CSVReader reader) {
		
    final int numCols = reader.getHeader().length;
    colNames = reader.getHeader();
		
		// create TableModel for the new JTable
		TableModel m = new AbstractTableModel() {
			
			private String[] columnNames = colNames;
			private Object[][] data = new Float[1][numCols];
			
			
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
			
			// If the input isn't parseable to float, just overwrite the last float in the cell
			@Override
			public void setValueAt(Object val, int row, int column ) {
				try{
					data[row][column] = new Float((String) val);
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
    
		for(int i=0; i<table.getColumnModel().getColumnCount(); i++) {
			TableColumn col = table.getColumnModel().getColumn(i);
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
	
	private JPanel createTimePointPanel(JTable table) {
		// Build panel, which contains the table
		JPanel panel = new JPanel();
		GUITools.createTitledPanel(panel, "Set time points");
			
		// Because the there is no need for a scroll pane, the header must be added manually
		panel.setLayout(new BorderLayout());
		panel.add(table.getTableHeader(), BorderLayout.PAGE_START);
		panel.add(table, BorderLayout.CENTER);
		
		return panel;	
	}

	@Override
	protected List<ExpectedColumn> getAdditionalExpectedColumns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void processAdditionalExpectedColumns(
			List<ExpectedColumn> additional) {
		// TODO Auto-generated method stub
		
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



/*
// User changes on the preview table are also made in the timePointsTable
JComponent previewPanel = c.getPreviewPanel();

// The table in the previewPanel, where we want to add changes like ActionListeners
JTableRowBased table = null;

// Get the table from the previewPanel
for(Component com : previewPanel.getComponents()) {
	if(com instanceof JScrollPane) {
		JScrollPane p = (JScrollPane) com;
		table = (JTableRowBased) p.getViewport().getView();
	}
}

// Get the tableModel
TableModel model = table.getModel();
int numCol = model.getColumnCount();

// Get the boxes
JComboBox[] boxes = new JComboBox[model.getColumnCount()];
for(int i=0; i<numCol; i++) {
	//System.out.println(model.getValueAt(0, i).getClass()); //JComboBoxes

	boxes[i] = (JComboBox) model.getValueAt(0, i);
		
	boxes[i].addActionListener(new ActionListener() {
	  /* (non-Javadoc)
	  * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	  */
/*
	  public void actionPerformed(ActionEvent e) {
	  	int index = ((JComboBox)e.getSource()).getSelectedIndex();
	    Object item = ((JComboBox)e.getSource()).getSelectedItem();
	    
	    System.out.println(item.toString());
	    ExpectedColumn col = (ExpectedColumn) item;
	    
	    
	  }
	});
}*/

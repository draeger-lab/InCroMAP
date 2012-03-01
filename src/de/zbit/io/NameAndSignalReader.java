/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
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
package de.zbit.io;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.integrator.ReaderCache;
import de.zbit.integrator.ReaderCacheElement;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

/**
 * A generic class to read data, consisting of name and signals.
 * E.g., mRNA or miRNA data.
 * <p>Note:<br/>Your extending class should have an empty
 * constructor, such that an instance of the reader can be created
 * and {@link #importWithGUI(String)} can be called.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class NameAndSignalReader<T extends NameAndSignals> implements Serializable {
  public static final transient Logger log = Logger.getLogger(NameAndSignalReader.class.getName());
  
  /**
   * Required: Column that contains the name
   */
  protected int nameCol;
  
  /**
   * Required: List of different signal columns
   * Integer=ColumnNumber, SignalType={@link SignalType}, String=ExperimentName/ObservationName
   */
  private Collection<ValueTriplet<Integer, SignalType, String>> signalColumns;
  
  
  /**
   * This reads any column, identifies by the integer and stores it with
   * a key, identified by the String.
   */
  private Collection<ValuePair<Integer, String>> additionalDataToRead=null;
  
  /**
   * This can also be used by extending classes.
   */
  protected transient AbstractProgressBar progress;
  
  /**
   * This should be used as a secondary progress bar. I.e.
   * if a temporary mapping file must be read, etc.
   * {@link #progress} should only be used for the real
   * input file reading progress, OR if this is null.
   */
  protected transient AbstractProgressBar secondaryProgress;
  
  /**
   * Decimal separator used to parse Numbers
   */
  private char decimalSeparator = '.';
  
  /**
   * Number of Signals that could not parse the number
   * (possibly wrong decimal separator).
   */
  private int errornousNumbersInSignals=0;
  /**
   * Number of Signals that could not parse the number,
   * but could possibly get fixed by changing to
   * {@link #getOtherDecimalSeparator()}.
   */
  private int errornousNumbersThatCouldBeFixed=0;
  /**
   * Total number of signals that could succesfully parse
   * a number.
   */
  private int totalNumbersParsed=0;
  
  /**
   * Import a file with a GUI. There are many helper methods that allow to quickly
   * implement this method:<ol><li>Create {@link ExpectedColumn}s by using
   * {@link #getExpectedSignalColumns(int)} and adding you own columns (especially
   * for {@link #nameCol}!
   * <li>Use {@link CSVImporterV2} to get the {@link CSVReader} and columns assignments
   * and build the reader. 
   * <li>Afterwards, return {@link #read(CSVReader)} using the
   * {@link CSVImporterV2#getApprovedCSVReader()}.
   * </ol>
   * @param parent the parent {@link Frame} or {@link Dialog}
   * @param file
   * @return
   */
  public Collection<T> importWithGUI(Component parent, String file) {
    return importWithGUI(parent, file, null);
  }
  
  /**
   * Import a file with a GUI. There are many helper methods that allow to quickly
   * implement this method:<ol><li>Create {@link ExpectedColumn}s by using
   * {@link #getExpectedSignalColumns(int)} and adding you own columns (especially
   * for {@link #nameCol}!
   * <li>Use {@link CSVImporterV2} to get the {@link CSVReader} and columns assignments
   * and build the reader. 
   * <li>Afterwards, return {@link #read(CSVReader)} using the
   * {@link CSVImporterV2#getApprovedCSVReader()}.
   * </ol>
   * @param parent the parent {@link Frame} or {@link Dialog}
   * @param file
   * @param cache Might be null! If not, try to read file input configuration from this cache
   * and put your configuration into this cache.
   * @return
   */
  public abstract Collection<T> importWithGUI(Component parent, String file, ReaderCache cache);
  
  /**
   * Can be used by extending classes to handle cache load queries.
   * @param cache current {@link ReaderCache} instance
   * @param file input file to read
   * @param exCol array of expected columns. May be null if none.
   * @param spec an Organism Selector (See
   * {@link IntegratorUITools#getOrganismSelector()}). May be null if not required.
   * @return a CSVReader, either initialized from scratch
   * (if cache was empty) or configured from the cache.
   * The selections of <code>exCol</code> and
   * <code>spec</code> are changed automatically in their instances.
   */
  protected CSVReader loadConfigurationFromCache(ReaderCache cache, String file,
    ExpectedColumn[] exCol, JLabeledComponent spec) {
    // Evaluate and load data from cache.
    File inputFile = new File(file);
    CSVReader inputReader = new CSVReader(file);
    if (cache!=null && cache.contains(inputFile)) {
      ReaderCacheElement ci = cache.get(inputFile);
      ci.configureReader(inputReader);
      if (exCol!=null) ci.configureExpectedColumns(exCol);
      if (spec !=null) ci.configureOrganismSelector(spec);
    }
    return inputReader;
  }
  
  public NameAndSignalReader(int nameCol) {
    super();
    signalColumns=null;
    this.nameCol = nameCol;
  }
  
  public NameAndSignalReader(int nameCol, Collection<ValueTriplet<Integer, SignalType, String>> signalColumns) {
    this(nameCol);
    this.signalColumns=signalColumns;
  }
  
  
  /*public void addSignalColumn(int col, String experimentName) {
    addSignalColumn(col, SignalType.Unknown, experimentName);
  }*/
  
  public void addSignalColumn(int col, SignalType type, String experimentName) {
    if (signalColumns==null) initializeSignalColumns();
    signalColumns.add(new ValueTriplet<Integer, SignalType, String>(col, type, experimentName));
  }
  
  private void initializeSignalColumns() {
    signalColumns = new ArrayList<ValueTriplet<Integer, SignalType, String>>();
  }
  
  /**
   * @return the (unmodifiable) list of signal columns
   */
  public Collection<ValueTriplet<Integer, SignalType, String>> getSignalColumns() {
    if (signalColumns==null) return null;
    return Collections.unmodifiableCollection(signalColumns);
  }
  
  /**
   * @return the numbert of signal columns
   */
  public int getNumberOfSignalColumns() {
    return signalColumns==null?0:signalColumns.size();
  }
  
  public void addAdditionalData(int col, String key) {
    if (additionalDataToRead==null) additionalDataToRead = new ArrayList<ValuePair<Integer, String>>();
    additionalDataToRead.add(new ValuePair<Integer, String>(col,key));
  }
  
  public void removeAdditionalData(int col, String key) {
    if (additionalDataToRead==null) return;
    additionalDataToRead.remove(new ValuePair<Integer, String>(col,key));
  }
  
  /**
   * @see #read(String)
   * @param file
   * @return
   * @throws Exception 
   * @throws IOException 
   */
  public Collection<T> read(File file) throws IOException, Exception {
    return read(file.getPath());
  }

  /**
   * @see #read(CSVReader)
   * @param inputCSVFile
   * @return
   * @throws IOException
   * @throws Exception
   */
  public Collection<T> read(String inputCSVFile) throws IOException, Exception {
    CSVReader r = new CSVReader(inputCSVFile);
    return read(r);
  }
  
  /**
   * Parses the first 25 lines and checks wether to use
   * {@link #getDecimalSeparator()} or {@link #getOtherDecimalSeparator()}
   * to parse numbers. Takes the one that occures more often.
   * @param r
   * @throws IOException
   */
  public void guessDecimalSeparator(CSVReader r) throws IOException {
    if (getNumberOfSignalColumns()>0) {
      // Peek in file
      r.open();
      String[] line;
      char decimalSeparator = getDecimalSeparator();
      char other = getOtherDecimalSeparator();
      int stringContainsDecSep=0;
      int stringContainsOtherDecSep=0;
      int counter = 0;
      while ((line=r.getNextLine())!=null) {
        
        // Check for separators
        for (ValueTriplet<Integer, SignalType, String> vp: signalColumns) {
          if (line.length>vp.getA()) {
            String s = line[vp.getA()];
            if (s.indexOf(decimalSeparator)>-1) {
              stringContainsDecSep++;
            }
            if (s.indexOf(other)>-1) {
              stringContainsOtherDecSep++;
            }
          }
        }
        
        if (counter>25) break; // is enough!
        counter++;
      }
      if (stringContainsOtherDecSep>stringContainsDecSep) {
        log.info(String.format("Changing decimal separator from '%s' to '%s'.", getDecimalSeparator(), getOtherDecimalSeparator()));
        setDecimalSeparator(getOtherDecimalSeparator());
      }
    }
  }
  
  /**
   * Read {@link NameAndSignals} data from given CSV file.
   * 
   * <p>Remark:<br/>
   * Your <T extends NameAndSignal> class should define its hashcode
   * and compareTo classes only by identifying the probe or "thing
   * represented by this object". It must *NOT* include the signal
   * list in hashcode calculations. This remark is only for hashCode
   * and compareTo. NOT for the equals method.</p>
   * @param inputCSV
   * @return
   * @throws IOException
   * @throws Exception - critical exception (not IO related) that make
   * reading the input data to the desired format impossible.
   */
  public Collection<T> read(CSVReader r) throws IOException, Exception {
    // Guess decimal separator
    guessDecimalSeparator(r);
    
    // Initialize everything
    init();
    r.setProgressBar(getProgressBar());
    r.setDisplayProgress(getProgressBar()!=null);
    r.open();
    Collection<T> ret = new ArrayList<T>(256);
    
    // Read all data
    String[] line;
    while ((line=r.getNextLine())!=null) {
      if (Thread.currentThread().isInterrupted()) break;
      processLine(line, ret);
    }
    
    // check if we should rather change the decimal separator and re-read
    if (isNumberParsingErrorsOccured() && isUsefulToChangeDecimalSeparator() && !Thread.currentThread().isInterrupted()) {
      log.info(String.format("Changing decimal separator from '%s' to '%s'.", getDecimalSeparator(), getOtherDecimalSeparator()));
      setDecimalSeparator(getOtherDecimalSeparator());
      ret.clear();
      r.open();
      while ((line=r.getNextLine())!=null) {
        if (Thread.currentThread().isInterrupted()) break;
        processLine(line, ret);
      }
    }
    // ---
    
    done(ret);
    ((ArrayList<T>)ret).trimToSize();
    return ret;
  }
  
  
  /**
   * This method can be overwritten to initialize anything,
   * before starting to actually read an input file.
   */
  protected void init() {
    // Intentionally left blank
  }

  /**
   * This method can be overwritten to un-initialize anything,
   * after all reading has been done.
   * @param ret 
   */
  protected void done(Collection<T> ret) {
    // Intentionally left blank
  }
  
  /**
   * This will set the {@link #nameCol} to zero and simply create a data
   * structure using the given <code>identifiers</code> as names.
   * <p>Very helpful, e.g., when reading a gene list.
   * @param identifiers
   * @return
   * @throws IOException
   * @throws Exception
   */
  public Collection<T> read(String[] identifiers) throws IOException, Exception {
    init();
    //Map<T, T> ret = new HashMap<T, T>();
    Collection<T> ret = new ArrayList<T>(128);
    
    nameCol = 0;
    for (String id : identifiers) {
      processLine(new String[]{id}, ret);
    }
    
    done(ret);
    return ret; //.values();
  }
  
  /**
   * Processes one line from the CSVReader.
   * @param line current line
   * @param ret current data that has been read
   * @throws Exception 
   */
  private void processLine(String[] line, Collection<T> ret) throws Exception {
    if (nameCol>=line.length) return; // continue;
    String name = getName(line);
    if (name==null) return; // continue;
    
    T m = createObject(name, line);
    if (m==null) return;
    
    // Assign additional data
    if (additionalDataToRead!=null) {
      for (ValuePair<Integer, String> vp : additionalDataToRead) {
        if (line.length>vp.getA()) {
          m.addData(vp.getB(), line[vp.getA()]);
        }
      }
    }
    
    // Get unique object to assign signals
//    T mi = ret.get(m);
//    if (mi==null) {
//      mi = m;
//      ret.put(mi, mi);
//    }
    T mi = m;
    ret.add(m);
    
    parseSignals(line, mi);
  }

  /**
   * An overridable method, for cases in which the {@link #nameCol}
   * might not be required.
   * @param line
   * @return
   */
  protected String getName(String[] line) {
    return line[nameCol];
  }

  /**
   * Parses the {@link Signal}s from the current csv-files line.
   * @param line current line
   * @param mi target object for writing the signal to
   */
  private void parseSignals(String[] line, NameAndSignals mi) {
    // Add signals
    char decimalSeparator = getDecimalSeparator();
    if (signalColumns!=null) {
      for (ValueTriplet<Integer, SignalType, String> vp: signalColumns) {
        if (line.length>vp.getA()) {
          if (decimalSeparator!='.') {
            line[vp.getA()]=line[vp.getA()].replace(decimalSeparator, '.');
          }
          // Float is sufficient for nearly all files and saves 50% heap space to doubles
          Float signal = Float.NaN;
          try {
            signal = Float.parseFloat(line[vp.getA()]);
          } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Error while parsing signal number.", e);
            errornousNumbersInSignals++;
            if (line[vp.getA()].indexOf(getOtherDecimalSeparator())>=0){
              // Count errors that could poentiatlly get fixed
              errornousNumbersThatCouldBeFixed++;
            }
          }
          mi.addSignal(signal, vp.getC(), vp.getB());
        }
      }
      totalNumbersParsed+=mi.getSignals().size();
    }
  }
  
  /**
   * @return <code>TRUE</code> if there were signals that could not
   * parse a number, for some reason ({@link NumberFormatException}
   * occured).
   * @see #errornousNumbersInSignals
   */
  public boolean isNumberParsingErrorsOccured() {
    return errornousNumbersInSignals>0;
  }
  
  /**
   * Analyzes the statistics for {@link NumberFormatException}s
   * and if those strings contained {@link #getOtherDecimalSeparator()}.
   * If this method returns true, you should change the {@link #decimalSeparator}
   * to {@link #getOtherDecimalSeparator()} and re-read the data.
   * @return true if you should change the decimal separator
   * and re-read all signals.
   */
  public boolean isUsefulToChangeDecimalSeparator() {
    if (totalNumbersParsed<10) return false; // dataset to small...
    // Check if we can fix errors by changing the decimal separator
    if (errornousNumbersInSignals>0 && errornousNumbersThatCouldBeFixed>0) {
      if ((errornousNumbersInSignals/(double)totalNumbersParsed)>=0.25) {
        // Only suggest to fix if we had at least 25% errors
        if ((errornousNumbersThatCouldBeFixed/(double)errornousNumbersInSignals)>=0.75) {
          // We could fix at least 75% of those errors
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @return
   */
  public char getOtherDecimalSeparator() {
    return getDecimalSeparator()=='.'?',':'.';
  }

  /**
   * @return character used to separate decimals
   * ('.' in english, ',' in german).
   */
  public char getDecimalSeparator() {
    return decimalSeparator;
  }
  
  /**
   * Change the decimal separator.
   * @param d
   */
  public void setDecimalSeparator(char d) {
    decimalSeparator = d;
  }

  /*
   *       String probeName = probeNameCol>=0?line[probeNameCol]:null;
      miRNA m = new miRNA(line[miRNAnameCol], probeName);
   */
  /**
   * Create your object from the CSV file, the name is already parsed and
   * also the signals is being taken care of. Additional values from the
   * CSV file can be parsed here. Else, the return is simply
   * <pre>
   * return new MyClass(name);
   * </pre>
   * <p>If you have to parse numbers, please use {@link #getDecimalSeparator()}
   * as decimal separator.</p>
   * @throws Exception only critical exceptions should be thrown that really
   * make reading the data impossible.
   */
  protected abstract T createObject(String name, String[] line) throws Exception;
  
  /**
   * Creates a collection of maxNumberOfObservations optional {@link ExpectedColumn}s
   * that accepts FoldChanges and pValues of renameable signals. This is optimal for 
   * importing {@link Signal} using the {@link CSVImporterV2}.
   * @param maxNumberOfObservations number of expected observation columns to create
   * @return
   */
  protected static Collection<ExpectedColumn> getExpectedSignalColumns(int maxNumberOfObservations) {
    // Application can (as of today) only process pValues and fold changes.
    SignalType[] types = new SignalType[]{SignalType.FoldChange, SignalType.pValue};
    
    Collection<ExpectedColumn> r = new ArrayList<ExpectedColumn>(maxNumberOfObservations); 
    for (int i=1; i<=maxNumberOfObservations; i++) {
      ExpectedColumn e = new ExpectedColumn("Observation " + i, types, false, true,true,true);
      r.add(e);
    }
    return r;
  }
  
  /**
   * Creates a collection of maxNumberOfColumns optional {@link ExpectedColumn}s
   * that accepts any additional data. This is optimal for 
   * importing {@link #addAdditionalData(int, String)} using the {@link CSVImporterV2}.
   * @param maxNumberOfColumns number of {@link ExpectedColumn}s to create
   * @return
   */
  protected static Collection<ExpectedColumn> getAdditionalDataColumns(int maxNumberOfColumns) {
    // Application can (as of today) only process pValues and fold changes.
    Collection<ExpectedColumn> r = new ArrayList<ExpectedColumn>(maxNumberOfColumns); 
    for (int i=1; i<=maxNumberOfColumns; i++) {
      ExpectedColumn e = new ExpectedColumn("Additional data " + i, null, false, false,false,true,null);
      r.add(e);
    }
    return r;
  }

  /**
   * @param progress
   */
  public void setProgressBar(AbstractProgressBar progress) {
    this.progress = progress;
  }

  /**
   * Please override this method and return a species, if it is known
   * while reading the data. Else, simply return null.
   * @return
   */
  public abstract Species getSpecies();

  /**
   * @return
   */
  public AbstractProgressBar getProgressBar() {
    return this.progress;
  }

  /**
   * @return the secondary progress bar if this is not null.
   * If the secondary progress bar is null, the primary
   * {@link #getProgressBar()} will be returned.
   */
  public AbstractProgressBar getSecondaryProgressBar() {
    return secondaryProgress!=null?secondaryProgress:getProgressBar();
  }

  /**
   * Set a secondary progress bar.
   * This is used if a temporary mapping file must be read, etc.
   * Whereas {@link #progress} is used for the real input file reading progress,
   * OR if this secondary bar is null.
   * @param secondaryProgress the secondaryProgress to set
   */
  public void setSecondaryProgressBar(AbstractProgressBar secondaryProgress) {
    this.secondaryProgress = secondaryProgress;
  }
  
  


}

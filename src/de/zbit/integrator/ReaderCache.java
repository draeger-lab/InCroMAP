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
package de.zbit.integrator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.io.NameAndSignalReader;
import de.zbit.util.Utils;

/**
 * A cache for configurations for the {@link NameAndSignalReader}.
 * Stores the reader to use, the organism and confirmed file/
 * column configurations for the {@link CSVImporterV2}. 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ReaderCache implements Serializable {
  private static final long serialVersionUID = -4076147191332720931L;
  
  /**
   * Filename of the KEGG cache file (implemented just
   * like the browser cache). Must be loaded upon start
   * and saved upon exit.
   */
  public final static String cacheFileName = "rsetCache.dat";
  
  /**
   * Keep one instance of the cache in memory.
   */
  private static ReaderCache instance=null;
  
  /**
   * Defina a maximum site for the number of elements in the cache.
   */
  private int maximumCacheSize;
  
  /**
   * The actual cache (sorted by age).
   */
  private Map<File, ReaderCacheElement> cache;
  
  /**
   * A currently running thread that is cleaning the cache.
   */
  private Thread cleaningCache=null;
  
  /**
   * A boolean flag to determine if the cache has changed.
   */
  private transient boolean cacheChangedSinceLoading=false;
  
  
  /**
   * @return current instance of the cache.
   */
  public synchronized static ReaderCache getCache() {
    // Try to load from cache file
    if (instance==null && new File(cacheFileName).exists() && new File(cacheFileName).length() > 1) {
      try {
        instance = loadFromFilesystem(cacheFileName);
      } catch (Throwable e) { // IOException or class cast, if class is moved.
        e.printStackTrace();
        // Delete invalid cache file
        try {
          File f = new File(cacheFileName);
          if (f.exists() && f.canRead()) {
            System.out.println("Deleting invalid cache file " + f.getName());
            f.delete();
          }
        } catch (Throwable t) {}
      }
    }
    
    // Create new, if loading failed
    if (instance==null) {
      instance = new ReaderCache(100);
    }
    
    return instance;
  }
  
  
  
  
  /**
   * Initializes a new cache instance with size 100.
   */
  public ReaderCache() {
    this (100);
  }
  
  /**
   * @param cacheSize maximum number of elements to cache
   */
  public ReaderCache(int cacheSize) {
    super();
    this.maximumCacheSize = cacheSize;
    this.cache = new HashMap<File, ReaderCacheElement>();
  }
  
  /**
   * Get a cached file descriptor
   * @param file
   * @return {@link ReaderCacheElement}
   */
  public ReaderCacheElement get(File file) {
    return cache.get(file);
  }
  
  /**
   * Add an element to the cache
   * @param file
   * @param fileDescriptor
   */
  public void add(File file, ReaderCacheElement fileDescriptor) {
    fileDescriptor.resetTime();
    fileDescriptor.setDescribingFile(file);
    
    // Check if cache changes with this addition
    ReaderCacheElement old = cache.get(file);
    if (old!=null) {
      // Not so easy to implement equals() method in
      // fileDescriptor AND expectedColumn.
      //if (old.equals(fileDescriptor)) return;
      cacheChangedSinceLoading = true;
    } else {
      cacheChangedSinceLoading = true;
    }
    
    // Put in cache and watch cache size
    stopCleaningCache();
    cache.put(file, fileDescriptor);
    ensureCacheSize();
  }
  
  /**
   * @param fileDescriptor
   * @see #add(File, ReaderCacheElement)
   */
  public void add(ReaderCacheElement fileDescriptor) {
    add(fileDescriptor.getDescribingFile(), fileDescriptor);
  }
  
  /**
   * Check if we have cached information for the given file.
   * @param file
   * @return true if information is available.
   */
  public boolean contains(File file) {
    return cache.containsKey(file) && cache.get(file)!=null;
  }
  
  /**
   * Cleans the oldest items from the cache to ensure a cache
   * size of maximum {@link #maximumCacheSize} items.
   */
  private void ensureCacheSize() {
    if (cache.size()<=maximumCacheSize) return;
    stopCleaningCache();
    Runnable cleaner = new Runnable() {
      @Override
      public void run() {
        int itemsToDelete = cache.size()-maximumCacheSize;
        if (itemsToDelete<1) return;
        List<ReaderCacheElement> list = new ArrayList<ReaderCacheElement>(cache.values());
        Collections.sort(list, ReaderCacheElement.getAgeComparator());
        // Oldest file is now fist (ascending sorting)
        for (int i=0; i<itemsToDelete; i++) {
          if (Thread.currentThread().isInterrupted() || list.size()<1) break;
          
          cache.remove(list.get(0).getDescribingFile());
        }
      }
    };
    cleaningCache = new Thread(cleaner);
    cleaningCache.start();
  }




  private void stopCleaningCache() {
    // Interrupt eventually running old thread.
    if (cleaningCache!=null && cleaningCache.isAlive()) {
      cleaningCache.interrupt();
    }
  }

  
  /**
   * @param file
   * @param fileDescriptor
   * @see #add(File, ReaderCacheElement)
   */
  public void put(File file, ReaderCacheElement fileDescriptor) {
    add (file, fileDescriptor);
  }



  /**
   * Load an instance of the cache from the filesystem.
   * @param filepath
   * @return loaded instance
   * @throws IOException 
   */
  public static synchronized ReaderCache loadFromFilesystem(String filepath) throws IOException {
    ReaderCache m = (ReaderCache) Utils.loadObjectAutoDetectZIP(filepath);
    return m;
  }

  /**
   * Save the given instance of the cache as serialized object.
   * @param filepath
   * @param m object to store.
   * @return true if and only if the file has been successfully saved.
   */
  public static synchronized boolean saveToFilesystem(String filepath, ReaderCache m) {
    return Utils.saveGZippedObject(filepath, m);
  }
  
  /**
   * Automatically determines if the cache has been initialized and changed.
   * If yes, it will be saved to {@value #cacheFileName}.
   * @return true if something needed and has been saved.
   */
  public static synchronized boolean saveIfRequired() {
    if (instance==null || !instance.cacheChangedSinceLoading) return false;
    else return saveToFilesystem(cacheFileName, instance);
  }
  
  
  /**
   * Add or replace an element to the cache.
   * @param c any configured and confirmed {@link CSVImporterV2}.
   */
  public void add(CSVImporterV2 c) {
    this.add(ReaderCacheElement.createInstance(c));
  }
  
}

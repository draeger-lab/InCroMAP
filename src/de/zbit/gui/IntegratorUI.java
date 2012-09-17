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
package de.zbit.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import de.zbit.AppConf;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.HeterogeneousNS;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.graph.gui.TranslatorGraphLayerPanel;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.graph.gui.options.TranslatorPanelOptions;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.actions.TranslatorTabActions;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.gui.prefs.IntegratorIOOptions;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.integrator.ReaderCache;
import de.zbit.io.DNAmethylationReader;
import de.zbit.io.EnrichmentReader;
import de.zbit.io.NameAndSignalReader;
import de.zbit.io.ProteinModificationReader;
import de.zbit.io.mRNAReader;
import de.zbit.io.miRNAReader;
import de.zbit.io.proxy.ProxySelection;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.ext.KEGGTranslatorPanelOptions;
import de.zbit.kegg.gui.IntegratorPathwayPanel;
import de.zbit.kegg.gui.TranslatePathwayDialog;
import de.zbit.kegg.gui.TranslatorGraphPanel;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.ValuePair;
import de.zbit.util.objectwrapper.ValueTriplet;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;
import de.zbit.visualization.VisualizeMicroRNAdata;

/**
 * A GUI for the Integrator application
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class IntegratorUI extends BaseFrame {
  private static final long serialVersionUID = 5599356490171620598L;
  public static final transient Logger log = Logger.getLogger(IntegratorUI.class.getName());
  
  /**
   * The name of the application.
   * 
   * <p>When renaming the application, also consider:
   * <li> src/overview.html
   * <li> resources/de/zbit/gui/html/about.html
   * <li> resources/de/zbit/gui/html/help.html
   * <li> various logo images
   */
  public final static String appName = "InCroMAP";
  
  /**
   * The version of {@link #appName}
   */
  public final static String appVersion = "1.2";
  
  /**
   * A simple light blue color. Used e.g. in {@link PathwayVisualizationOptions}
   * and in {@link VisualizeMicroRNAdata}.
   */
  public static final Color LIGHT_BLUE = new Color(0,153,255);
  
  /**
   * Since only one instance is allowed to show at a time,
   * this is a convenient method to get the current instance.
   */
  public static IntegratorUI instance = null;

  /**
   * Since only one instance is allowed to show at a time,
   * this is a convenient method to get the current instance.
   */
  public static IntegratorUI getInstance() {
    return instance;
  }
  
  /**
   * This is the main component
   */
  private JTabbedPane tabbedPane;
  
  /**
   * This map is required, because KEGGtranslator has no dynamic changing
   * Toolbar!
   */
  private Map<TranslatorGraphPanel, TranslatorTabActions> translatorActionMap = new HashMap<TranslatorGraphPanel, TranslatorTabActions>();
  
  /**
   * Holds a duplicate of the list of recently opened files.
   * Strangely, this does not work if it is not static.
   */
  private static JMenu fileHistoryDuplicate=null;
  
  /**
   * Default directory path's for saving and opening files. Only init them
   * once. Other classes should use these variables.
   */
  public static String openDir, saveDir;
  
  /**
   * preferences is holding all project specific preferences
   */
  private SBPreferences prefsIO;
  
  /**
   * This label should always display the species of a currently selected tab.
   */
  private JLabel speciesLabel;
  
  public static enum Action implements ActionCommand {
    /**
     * {@link Action} to save the currently opened tabs.
     */
    WORKSPACE_SAVE,
    /**
     * {@link Action} to load opened tabs from another session.
     */
    WORKSPACE_LOAD,
    /**
     * {@link Action} to load an enrichment (from external applications).
     */
    LOAD_ENRICHMENT,
    /**
     * {@link Action} to load data with the {@link mRNAReader}.
     */
    LOAD_MRNA,
    /**
     * {@link Action} to load data with the {@link miRNAReader}.
     */
    LOAD_MICRO_RNA,
    /**
     * {@link Action} to load data with the {@link ProteinModificationReader}.
     */
    LOAD_PROTEIN_MODIFICATION,
    /**
     * {@link Action} to load data with the {@link DNAMethylationReader}
     */
    LOAD_DNA_METHYLATION,
    /**
     * {@link Action} to let the user enter a custom gene list.
     */
    INPUT_GENELIST,
    /**
     * {@link Action} to show raw {@link miRNAtargets}.
     */
    SHOW_MICRO_RNA_TARGETS,
    /**
     * {@link Action} to show a pathway selection dialog. 
     */
    NEW_PATHWAY,
    /**
     * Creates pictures for many observations and pathways.
     */
    BATCH_PATHWAY_VISUALIZATION,
    /**
     * Show different data of different types in one pathway.
     */
    INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION,
    /**
     * Show an integrated TreeTable of various data types.
     */
    INTEGRATED_TABLE,
    /**
     * Perform an integrated enrichment analysis.
     */
    INTEGRATED_ENRICHMENT;
    
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case WORKSPACE_SAVE:
        return "Save workspace";
      case WORKSPACE_LOAD:
        return "Load workspace";
      case LOAD_MRNA:
        return "Load mRNA data";
      case LOAD_MICRO_RNA:
        return "Load microRNA data";
      case LOAD_PROTEIN_MODIFICATION:
        return "Load protein modification data";
      case LOAD_DNA_METHYLATION:
        return "Load DNA methylation data";
      case INPUT_GENELIST:
        return "Manually enter a gene list";
      case SHOW_MICRO_RNA_TARGETS:
        return "Show microRNA targets";
      case NEW_PATHWAY:
        return "Show pathway";
      case INTEGRATED_TABLE:
        return "Integrate heterogeneous data";
        
      default:
        return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    public String getToolTip() {
      switch (this) {
        case WORKSPACE_SAVE:
          return "Save all currently opened tabs to disk.";
        case WORKSPACE_LOAD:
          return "Load opened tabs from a previous session.";
        case LOAD_MRNA:
          return "Load processed mRNA data from file.";
        case LOAD_MICRO_RNA:
          return "Load processed microRNA data from file.";
        case LOAD_PROTEIN_MODIFICATION:
          return "Load processed protein modification expression data from a file.";
        case LOAD_DNA_METHYLATION:
          return "Load processed and gene-centered DNA methylation data from a file.";
        case INPUT_GENELIST:
          return "Manually enter a list of genes.";
        case SHOW_MICRO_RNA_TARGETS:
          return "Show raw microRNA targets for an organism.";
        case NEW_PATHWAY:
          return "Download and visualize a KEGG pathway.";
        case BATCH_PATHWAY_VISUALIZATION:
          return "Batch color nodes in various pathways according to observations and save these pictures as images.";
        case INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION:
          return "Visualize heterogeneous data from different datasets in one pathway.";
        case INTEGRATED_TABLE:
          return "Integrate multiple heterogeneous datasets from different platforms by building a gene-centered tree.";
        case INTEGRATED_ENRICHMENT:
          return "Perform an integrated enrichment across multiple (heterogeneous) datasets.";
        case LOAD_ENRICHMENT:
          return "Load an enrichment that has been saved from this or any other application (e.g., DAVID, GSEA, or others).";
          
        default:
          return "";
      }
    }
    
  }
  
  /**
   * @return list of command-line options.
   */
  public static List<Class<? extends KeyProvider>> getStaticCommandLineOptions() {
    List<Class<? extends KeyProvider>> configList = new LinkedList<Class<? extends KeyProvider>>();
    //configList.add(IntegratorIOOptions.class);
    configList.add(GUIOptions.class);
    //configList.add(PathwayVisualizationOptions.class);
    return configList;
  }

  /**
   * This method changes some default values of KEGGtranslator,
   * option visibilities, logos, etc. to look like it would be
   * the {@link IntegratorUI} application.
   */
  public static void integrateIntoKEGGtranslator() {
    // Set default values for KEGGtranslator
    KEGGtranslatorOptions.REMOVE_ORPHANS.setDefaultValue(false);
    KEGGtranslatorOptions.REMOVE_WHITE_GENE_NODES.setDefaultValue(false);
    KEGGtranslatorOptions.SBML_OPTIONS.setVisible(false);
    
    KEGGtranslatorOptions.AUTOCOMPLETE_REACTIONS.setVisible(false);
    KEGGtranslatorOptions.AUTOCOMPLETE_REACTIONS.setDefaultValue(false);
    
    TranslatorPanelOptions.SHOW_PROPERTIES_TABLE.setDefaultValue(false);
    TranslatorPanelOptions.DRAW_EDGES_ON_TOP_OF_NODES.setDefaultValue(true);
    // Edge layouting will create fixed bends in edges that obscure the graph,
    // if nodes are moved for any reason.
    //TranslatorPanelOptions.LAYOUT_EDGES.setDefaultValue(true);
    
    // Set the often used KeggTranslator methods to use this appName as application name
//    Translator.APPLICATION_NAME = appName;
//    Translator.VERSION_NUMBER = appVersion;
    
    TranslatorUI.watermarkLogoResource = "img/logo.png";
    TranslatorGraphLayerPanel.optionClass = KEGGTranslatorPanelOptions.class;
  }
  
  /**
   * Load icons in static constructor
   */
  static {
    String iconPaths[] = {"IntegratorIcon_16.png","IntegratorIcon_32.png","IntegratorIcon_48.png","IntegratorIcon_128.png","IntegratorIcon_256.png"
        ,"ICON_PATHWAY_16.png", "ICON_MATH_16.png", "IntegratorIcon_16_straight.png"
        ,"ICON_MSIGDB_16.png", "ICON_GO_16.png", "ICON_KEGG_16.png"
        ,"ICON_MRNA_16.png", "ICON_MIRNA_16.png", "ICON_DNAM_16.png", "ICON_PROTEIN_16.png"};
    for (String path : iconPaths) {
      URL url = IntegratorUI.class.getResource("img/" + path);
      if (url!=null) {
        UIManager.put(path.substring(0, path.lastIndexOf('.')), new ImageIcon(url));
      }
    }
  }
  
  public IntegratorUI() {
    this(null);
  }
  
  /**
   * @param appConf
   */
  public IntegratorUI(AppConf appConf) {
    super (appConf);
    
    // init preferences
    initPreferences();
    
    // Load reader cache
    ReaderCache.getCache();
        
    // Depending on the current OS, we should add the following image
    // icons: 16x16, 32x32, 48x48, 128x128 (MAC), 256x256 (Vista).
    int[] resolutions=new int[]{16,32,48,128,256};
    List<Image> icons = new LinkedList<Image>();
    for (int res: resolutions) {
      Object icon = UIManager.get("IntegratorIcon_"+res);
      if ((icon != null) && (icon instanceof ImageIcon)) {
        icons.add(((ImageIcon) icon).getImage());
      }
    }
    setIconImages(icons);
    
    // Adds a label for the species and data type of the current tab to the status bar.
    initStatusBarSpeciesLabel();
    
    /*
     * Many tooltips contain descriptions that must be shown
     * longer than the system default. Let's show them 30 seconds!
     */
    ToolTipManager.sharedInstance().setDismissDelay(30000);
    
    instance = this;
    
    // Eventually load previously stored proxy server settings.
    new ProxySelection.Tools().initializeProxyServer();
  }

  /**
   * Init preferences, if not already done.
   */
  private void initPreferences() {
    if (prefsIO == null) {
      prefsIO = SBPreferences.getPreferencesFor(IntegratorIOOptions.class);
    }
    TranslatorGraphLayerPanel.optionClass = KEGGTranslatorPanelOptions.class;
  }
  
  /**
   * Places a new label on the {@link #getStatusBar()}s right side,
   * displaying the species of each selected tab.
   */
  private void initStatusBarSpeciesLabel() {
    // Create a smaller panel for the statusBar
    Dimension panelSize = new Dimension(275, 15);
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setOpaque(false);
    panel.setPreferredSize(panelSize);
    speciesLabel = new JLabel();
    speciesLabel.setOpaque(false);
    speciesLabel.setHorizontalTextPosition(JLabel.RIGHT);
    panel.add(speciesLabel);
    
    statusBar.getRightPanel().add(panel, BorderLayout.CENTER);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#closeFile()
   */
  @Override
  public boolean closeFile() {
    int idx = tabbedPane.getSelectedIndex();
    if (idx>=0) {
      return closeTab(idx);
    }
    return false;
  }
  
  /**
   * Close the given tab from the {@link #tabbedPane}.
   * @param tab
   * @return true if and only if the tab has been removed from the tabbedPane.
   */
  public boolean closeTab(Component tab) {
    return closeTab(getTabIndex(tab));
  }
  
  /**
   * Cloase tab at specified index.
   * @param tabIndex
   * @return
   */
  public boolean closeTab(int tabIndex) {
    try {
      // Remove tab-related action
      if (tabbedPane.getComponentAt(tabIndex) instanceof TranslatorPanel) {
        translatorActionMap.remove(tabbedPane.getComponentAt(tabIndex));
      }
      // Close tab
      tabbedPane.removeTabAt(tabIndex);
      
      updateButtons();
      return true;
    } catch (Exception e) {
      log.log(Level.WARNING, "Could not close tab.", e);
    }
    return false;
  }
  
  /**
   * Returns the index of the <code>tab</code> in the
   * {@link #tabbedPane}.
   * @param tab
   * @return -1 if not found, else the index.
   */
  public int getTabIndex(Component tab) {
    if (tab==null) return -1;
    for (int i=0; i<tabbedPane.getTabCount(); i++) {
      Component c = tabbedPane.getComponentAt(i);
      if (c!=null && c.equals(tab)) {
        return i;
      }
    }
    return -1;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#additionalFileMenuItems()
   */
  @Override
  protected JMenuItem[] additionalFileMenuItems() {
    
    JMenu importData = new JMenu("Import enrichment result");
    importData.setToolTipText(Action.LOAD_ENRICHMENT.getToolTip());
    
    /*
     * Make separate menu items for the same thing (requested by reviewers).
     */
    
    importData.add(
      GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openEnrichment"),
        new ActionCommand() {
        @Override
        public String getToolTip() {
          return "Load an enrichment that has been saved with this tool.";
        }
        @Override
        public String getName() {
          return "Load InCroMAP enrichment";
        }
      }, UIManager.getIcon("ICON_OPEN_16"))
    );
    
    importData.add(
      GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openEnrichment"),
        new ActionCommand() {
        @Override
        public String getToolTip() {
          return "Load an enrichment that has been saved with the official GSEA tool (see http://www.broadinstitute.org/gsea/index.jsp).";
        }
        @Override
        public String getName() {
          return "Load GSEA enrichment";
        }
      }, UIManager.getIcon("ICON_OPEN_16"))
    );
    
    importData.add(
      GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openEnrichment"),
        new ActionCommand() {
        @Override
        public String getToolTip() {
          return "Load an enrichment that has been saved with DAVID Bioinformatics Resources (see http://david.abcc.ncifcrf.gov/).";
        }
        @Override
        public String getName() {
          return "Load DAVID enrichment";
        }
      }, UIManager.getIcon("ICON_OPEN_16"))
    );
    
    importData.add(new JSeparator());
    
    importData.add(
      GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openEnrichment"),
        new ActionCommand() {
        @Override
        public String getToolTip() {
          return "Load an enrichment that has been saved with any application or tool.";
        }
        @Override
        public String getName() {
          return "Load other enrichment";
        }
      }, UIManager.getIcon("ICON_OPEN_16"))
    );
    
    return new JMenuItem[] {
        /* TOO difficult to save workspace...
         * GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "saveWorkspace"),
          Action.WORKSPACE_SAVE, UIManager.getIcon("ICON_SAVE_16"), KeyStroke.getKeyStroke('W',InputEvent.CTRL_DOWN_MASK),
          'W', false)*/
    
      importData
    };
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#additionalEditMenuItems()
   */
  @Override
  protected JMenuItem[] additionalEditMenuItems() {
    // Additional PROXY SERVER settings dialog
    JMenuItem proxyMenu = GUITools.createJMenuItem(
      EventHandler.create(ActionListener.class, new ProxySelection.Tools(), "showDialog"),
      new ActionCommand() {
      @Override
      public String getToolTip() {
        return "Configure a proxy server.";
      }
      @Override
      public String getName() {
        return "Proxy server";
      }
    }, UIManager.getIcon("ICON_PREFS_16"));
    
    return new JMenuItem[]{proxyMenu};
  }
  
  @Override
  protected JMenu[] additionalMenus() {
    
    /*
     * Import data tab.
     */
    JMenu importData = new JMenu("Import data");
    
    JMenuItem lmRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMRNAfile"),
        Action.LOAD_MRNA, UIManager.getIcon("ICON_MRNA_16"));
      
    JMenuItem lmiRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMiRNAfile"),
        Action.LOAD_MICRO_RNA, UIManager.getIcon("ICON_MIRNA_16"));
    
    JMenuItem lprotMod = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openProteinModificationExpressionFile"),
      Action.LOAD_PROTEIN_MODIFICATION, UIManager.getIcon("ICON_PROTEIN_16"));
    
    JMenuItem ldnaM = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openDNAmethylationFile"),
      Action.LOAD_DNA_METHYLATION, UIManager.getIcon("ICON_DNAM_16"));
      
    JMenuItem genelist = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "showInputGenelistDialog"),
        Action.INPUT_GENELIST, UIManager.getIcon("ICON_OPEN_16"));

    JMenuItem miRNAtargets = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "showMicroRNAtargets"),
        Action.SHOW_MICRO_RNA_TARGETS, UIManager.getIcon("ICON_GEAR_16"));

    JMenuItem newPathway = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openPathwayTab"),
        Action.NEW_PATHWAY, UIManager.getIcon("ICON_PATHWAY_16"));

    importData.add(lmRNA);
    importData.add(lmiRNA);
    importData.add(lprotMod);
    importData.add(ldnaM);
    importData.addSeparator();
    importData.add(genelist);
    importData.add(miRNAtargets);
    importData.add(newPathway);
    
    /*
     * Tools tab.
     */
    JMenu tools = new JMenu("Tools");
    tools.add(GUITools.createJMenuItem(EventHandler.create(ActionListener.class, new IntegratorUITools(), "showBatchPathwayDialog"),
      Action.BATCH_PATHWAY_VISUALIZATION, UIManager.getIcon("ICON_PATHWAY_16")));
    tools.add(GUITools.createJMenuItem(EventHandler.create(ActionListener.class, new IntegratorUITools(), "showIntegratedVisualizationDialog"),
      Action.INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION, UIManager.getIcon("ICON_PATHWAY_16")));
    
    tools.add(GUITools.createJMenuItem(EventHandler.create(ActionListener.class, new IntegratorUITools(), "showIntegratedTreeTableDialog"),
      Action.INTEGRATED_TABLE, UIManager.getIcon("IntegratorIcon_16_straight")));
    
    tools.add(GUITools.createJMenuItem(EventHandler.create(ActionListener.class, new IntegratorUITools(), "showIntegratedEnrichmentDialog"),
      Action.INTEGRATED_ENRICHMENT, UIManager.getIcon("IntegratorIcon_16_straight")));
    
    
    
    return new JMenu[]{importData, tools};
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createJToolBar()
   */
  @Override
  protected JToolBar createJToolBar() {
    final JToolBar r = new JToolBar(appName+" toolbar", JToolBar.HORIZONTAL);
    r.setLayout(new GridLayout());
    return createJToolBarForNoTabs(r);
  }
  
  /**
   * Place buttons for loading items, etc. on the {@link JToolBar}.
   * This is intended to show up when no tab is selected, i.e., the
   * {@link #tabbedPane} is empty.
   * @param r
   * @return
   */
  protected JToolBar createJToolBarForNoTabs(JToolBar r) {
    
    // Place buttons, depending on current panel
    Object o = getCurrentlySelectedPanel();
    if (o!=null) {
      if (o instanceof BaseFrameTab) {
        ((BaseFrameTab)o).updateButtons(getJMenuBar(), r);
      } else {
        log.severe("Implement toolbar for " + o.getClass());
      }
    } else {
      if (r.getName().equals(getClass().getSimpleName())) return r; //Already done.
      r.removeAll();
      r.setName(getClass().getSimpleName());
      
      // Create several loadData buttons
      JPopupMenu load = new JPopupMenu("Load data");
      JMenuItem lmRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMRNAfile"),
        Action.LOAD_MRNA, UIManager.getIcon("ICON_MRNA_16"));
      load.add(lmRNA);
      JMenuItem lmiRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMiRNAfile"),
        Action.LOAD_MICRO_RNA, UIManager.getIcon("ICON_MIRNA_16"));
      load.add(lmiRNA);
      JMenuItem lprotMod = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openProteinModificationExpressionFile"),
        Action.LOAD_PROTEIN_MODIFICATION, UIManager.getIcon("ICON_PROTEIN_16"));
      load.add(lprotMod);
      JMenuItem ldnaM = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openDNAmethylationFile"),
        Action.LOAD_DNA_METHYLATION, UIManager.getIcon("ICON_DNAM_16"));
      load.add(ldnaM);
      load.addSeparator();
      if (fileHistoryDuplicate==null) {
        fileHistoryDuplicate = createFileHistory();
      }
      load.add(fileHistoryDuplicate);
      JDropDownButton loadDataButton = new JDropDownButton("Load data", UIManager.getIcon("ICON_OPEN_16"), load);
      
      JButton genelist = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "showInputGenelistDialog"),
        Action.INPUT_GENELIST, UIManager.getIcon("ICON_OPEN_16"));
      
      JButton miRNAtargets = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "showMicroRNAtargets"),
        Action.SHOW_MICRO_RNA_TARGETS, UIManager.getIcon("ICON_GEAR_16"));
      
      JButton newPathway = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "openPathwayTab"),
        Action.NEW_PATHWAY, UIManager.getIcon("ICON_PATHWAY_16"));
      
      r.add(loadDataButton);
      r.add(genelist);
      r.add(miRNAtargets);
      r.add(newPathway);
    }
    
    GUITools.setOpaqueForAllElements(r, false);
    return r;
  }
  
  /**
   * Add a single file to the history.
   * @param f
   */
  private void addToFileHistory(File... f) {
    if (f==null || f.length<1) return;
    List<File> l = new LinkedList<File>();
    for (File file : f) {
      if (file!=null) l.add(file);
    }
    if (l.size()<1) return;
    
    addToFileHistory(l, fileHistoryDuplicate);
  }
  
  public void openEnrichment() {
    openFile(null, false, EnrichmentReader.class);
  }
  
  public void openMRNAfile() {
    openFile(null, mRNAReader.class);
  }

  public void openProteinModificationExpressionFile() {
    openFile(null, ProteinModificationReader.class);
  }
  
  public void openDNAmethylationFile() {
    openFile(null, DNAmethylationReader.class);
  }
  
  public void openMiRNAfile() {
    openFile(null, miRNAReader.class);
  }
  
  /**
   * Shows a custom input genelist dialog to the user and adds this
   * list of genes to the {@link #tabbedPane}.
   */
  public void showInputGenelistDialog() {
    ValueTriplet<Species, IdentifierType, String> input = IntegratorUITools.showInputGeneListDialog();
    if (input==null) return; // Cancelled
    
    // Initialize reader
    NameAndSignalReader<mRNA> nsreader = new mRNAReader(0, input.getB(), input.getA());
    
    // Show import dialog and read data
    String[] genes = input.getC().split("\n");
    addTab(new NameAndSignalsTab(this, nsreader, genes, input.getA()), "Custom gene list (" + genes.length + " genes)");
    getStatusBar().showProgress(nsreader.getProgressBar());
    
  }
  
  /**
   * Asks the user for an organism and opens a tab with all known microRNA targets
   * for this organism.
   */
  public void showMicroRNAtargets() {
    ValuePair<miRNAtargets, Species> vp = IntegratorUITools.loadMicroRNAtargets(null);
    if (vp==null || vp.getA()==null) return; // Cancel pressed

    // Add as tab and annotate symbols in background
    NameAndSignalsTab nsTab = new NameAndSignalsTab(this, vp.getA(), vp.getB());
    addTab(nsTab, "miRNA targets (" + vp.getB().getCommonName() + ")");
    nsTab.getActions().showGeneSymbols_InNewThread();
  }
  
  public void openPathwayTab() {
    //Create the translator panel
    KEGGPathwayActionListener kpal = new KEGGPathwayActionListener(null);
//    TranslatorPanel pwTab = new TranslatorPanel(Format.JPG, kpal);
//    pwTab.addPropertyChangeListener(kpal);
//    addTab(pwTab, "Pathway"); 
    TranslatePathwayDialog d = new TranslatePathwayDialog(Format.JPG);
    d.setTranslatorPanelClassToInitialize(IntegratorPathwayPanel.class);
    TranslatePathwayDialog.showAndEvaluateDialog(tabbedPane, kpal, d);   
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createMainComponent()
   */
  @Override
  protected Component createMainComponent() {
    ImageIcon logo = new ImageIcon(IntegratorUI.class.getResource("img/logo.png"));
//    tabbedPane = new JTabbedLogoPane(logo);
    tabbedPane = new JTabbedPaneDraggableAndCloseable(logo);
    ((JTabbedPaneDraggableAndCloseable)tabbedPane).setShowCloseIcon(false);
    
    // Change active buttons, based on selection.
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        // Sometimes, external methods (e.g. from KEGGtranslator) add tabs
        // without Icons. Add them here, later on if they are missing.
        int idx = tabbedPane.getSelectedIndex();
        if (idx>=0 && tabbedPane.getIconAt(idx)==null) {
          tabbedPane.setIconAt(idx, IntegratorUITools.inferIconForTab(tabbedPane.getComponentAt(idx)));
        }
        
        // This is the real todo everytime the selected tab changes
        updateButtons();
      }
    });
    return tabbedPane;
  }
  
  /**
   * Enables and disables buttons in the menu, depending on the current tabbed
   * pane content.
   */
  public void updateButtons() {
    GUITools.setEnabled(false, getJMenuBar(), BaseAction.FILE_SAVE_AS, BaseAction.FILE_CLOSE);
    BaseFrameTab o = getCurrentlySelectedPanel();
    Species currentSpecies = null;
    String panelDataContent = null; // E.g., "mRNA"
    if (o != null) {
      if (o instanceof TranslatorPanel) {
        
        // Try to get species from graph panel
//        String specKegg = TranslatorTools.getOrganismKeggAbbrFromGraph((Graph2D) ((TranslatorPanel) o).getDocument());
//        if (specKegg!=null) {
//          currentSpecies = Species.search(IntegratorUITools.organisms, specKegg, Species.KEGG_ABBR);
//        }
        currentSpecies = TranslatorTabActions.getSpeciesOfPathway((TranslatorGraphPanel) o, IntegratorUITools.organisms);
        // ----
        
        TranslatorTabActions actions = translatorActionMap.get(o);
        if (actions==null) {
          actions = new TranslatorTabActions((TranslatorGraphPanel)o);
          translatorActionMap.put((TranslatorGraphPanel) o, actions);
        }
        actions.createJToolBarItems(toolBar);
        actions.updateToolbarButtons(toolBar);
      } else if (o instanceof IntegratorTab) {
        currentSpecies = ((IntegratorTab<?>) o).getSpecies(false);
        if (o instanceof NameAndSignalsTab) {
          try {
            Class<?> cl = ((NameAndSignalsTab) o).getDataContentType();
            
            panelDataContent = getShortTypeNameForNS(cl);
            
          } catch (Exception e) {
            panelDataContent = null;
          }
        }
      }
      
      o.updateButtons(getJMenuBar(), getJToolBar());
    } else {
      // Reset toolbar
      createJToolBarForNoTabs(toolBar);
    }
    
    // Display the current species.
    String specLabel = currentSpecies!=null?"Species: " + currentSpecies.getName():"";
    if (panelDataContent!=null) specLabel = "Type: " + panelDataContent + (specLabel.equals("")?"":", " + specLabel);
    if (speciesLabel!=null) speciesLabel.setText(specLabel);
  }

  /**
   * @param cl should be any {@link NameAndSignals} derived class.
   * @return a short name, describing the data type, MAY ALSO RETURN NULL!
   */
  public static String getShortTypeNameForNS(Class<?> cl) {
    String panelDataContent;
    // Translate class name to a nice and simple data type name.
    if (cl==null) panelDataContent = null; 
    else if (EnrichmentObject.class.isAssignableFrom(cl)) panelDataContent = "Enrichment";
    else if (PairedNS.class.isAssignableFrom(cl)) panelDataContent = null;
    else if (HeterogeneousNS.class.isAssignableFrom(cl)) panelDataContent = null;
    else if (cl.equals(NameAndSignals.class)) panelDataContent = null; // Temp panels return NS
    else if (ProteinModificationExpression.class.isAssignableFrom(cl)) panelDataContent = "Protein";
    else if (DNAmethylation.class.isAssignableFrom(cl)) panelDataContent = "DNA methylation";
    else if (miRNA.class.isAssignableFrom(cl)) panelDataContent = "miRNA"; // Also includes derivates (miRNAandTarget)
    else if (NameAndSignals.class.isAssignableFrom(cl)) panelDataContent = cl.getSimpleName();
    else panelDataContent = null; // Don't say "Object" or such
    return panelDataContent;
  }
  
  /**
   * @return the currently selected panel from the
   *         {@link #tabbedPane}, or null if either no or no valid selection
   *         exists.
   */
  private BaseFrameTab getCurrentlySelectedPanel() {
    if ((tabbedPane == null) || (tabbedPane.getSelectedIndex() < 0)) {
      return null;
    }
    Object o = ((JTabbedPane) tabbedPane).getSelectedComponent();
    
    if (o instanceof BaseFrameTab) {
      return (BaseFrameTab)o;
    } else {
      log.severe("Let " +  o.getClass() + " implement BaseFrameTab!");
      return null;
    }
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#exit()
   */
  @Override
  public void exit() {
    // Close the app and save caches.
    setVisible(false);
    try {
      Translator.saveCache();
      ReaderCache.saveIfRequired();
      
      SBProperties props = new SBProperties();
      if (openDir != null && openDir.length() > 1) {
        props.put(GUIOptions.OPEN_DIR, openDir);
      }
      if (saveDir != null && saveDir.length() > 1) {
        props.put(GUIOptions.SAVE_DIR, saveDir);
      }
      if (props.size()>0) {
        SBPreferences.saveProperties(GUIOptions.class, props);
      }
        
    } catch (BackingStoreException exc) {
      exc.printStackTrace();
      // Unimportant error... don't bother the user here.
      // GUITools.showErrorMessage(this, exc);
    }
    dispose();
    System.exit(0);
    
  }
  
  /*
   * (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLAboutMessage()
   */
  public URL getURLAboutMessage() {
    return IntegratorUI.class.getResource("html/about.html");
  }

  /*
   * (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLLicense()
   */
  public URL getURLLicense() {
    return IntegratorUI.class.getResource("html/license.html");
  }

  /*
   * (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLOnlineHelp()
   */
  public URL getURLOnlineHelp() {
    return IntegratorUI.class.getResource("html/help.html");
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
   */
  @Override
  protected File[] openFile(File... files) {
    return openFile(files, (Class<?>) null);
  }
  
  protected File[] openFile(File[] files, Class<?>... reader) {
    return openFile(files, true, reader);
  }
  protected File[] openFile(File[] files, boolean addToHistory, Class<?>... reader) {

    // Ask input file
    if ((files == null) || (files.length < 1)) {
      files = GUITools.openFileDialog(this, openDir, false, true,
        JFileChooser.FILES_ONLY, (FileFilter[]) null);
    }
    if ((files == null) || (files.length < 1)) return files;
    else {
      // Update openDir.
      if (files[0].getParent().length()>0) {
        openDir = files[0].getParent();
      }
    }
    
    // Ask file format
    if ( (reader == null) || (reader.length < 1) || (reader.length==1 && reader[0]==null)) {
      reader = new Class[files.length];
      for (int i=0; i<files.length; i++) {
        reader[i] = IntegratorUITools.createInputDataTypeChooser(files[i]);
      }
    }
    
    // Read all files and add tabs
    for (int i=0; i<files.length; i++) {
      try {
        // Initialize reader
        Class<?> r = (reader!=null&&i<reader.length)?reader[i]:null;
        if (r==null) r = IntegratorUITools.createInputDataTypeChooser(files[i]);
        if (r!=null) {
          NameAndSignalReader<? extends NameAndSignals> nsreader = (NameAndSignalReader<?>) r.newInstance();
          
          // Show import dialog and read data
          addTab(new NameAndSignalsTab(this, nsreader, files[i].getPath()), files[i].getName());
          getStatusBar().showProgress(nsreader.getProgressBar());
        } else {
          files[i]=null; // Dont add to history.
        }
      } catch (Exception e) {
        GUITools.showErrorMessage(this, e);
        files[i]=null; // Dont add to history.
      }
    }
    
    /* History has to be logged here, beacuse 
     * 1) if this method is invoked by e.g. "openMRNAfile()", it is not logged and
     * 2) the additionalHistory has to be changed too.
     */
    if (addToHistory) {
      addToFileHistory(files);
    }
    
    return files;
  }
  
  /**
   * Add a new tab to the {@link #tabbedPane}
   * @param tab
   * @param name
   */
  public void addTab(Component tab, String name) {
    addTab(tab,name,null);
  }
  
  /**
   * 
   * @param tab
   * @param name
   * @param toolTip
   */
  public void addTab(Component tab, String name, String toolTip) {
    addTab(tab, name, toolTip, null);
  }
  
  /**
   * Add a new tab to the {@link #tabbedPane}
   * @param tab
   * @param name
   * @param toolTip
   * @param icon
   */
  public void addTab(Component tab, String name, String toolTip, Icon icon) {
    tab.setName(name);
    if (icon==null) icon = IntegratorUITools.inferIconForTab(tab);
    tabbedPane.addTab(name, icon, tab, toolTip);
    tabbedPane.setSelectedComponent(tab);
  }
  
  /**
   * Allows to auto-adjust the icon of the tab, dependent on the content.
   * @param tab
   */
  public void setIconForTab(IntegratorTab<?> tab) {
    tabbedPane.setIconAt(getTabIndex(tab), IntegratorUITools.inferIconForTab(tab));
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#saveFile()
   */
  @Override
  public File saveFile() {
    Object o = getCurrentlySelectedPanel();
    if (o==null) return null;
    
    if (o instanceof BaseFrameTab) {
      File f = ((BaseFrameTab)o).saveToFile();
      if (f!=null) {
        // Update saveDir.
        saveDir = f.getParent();
      }
      return f;
      
    } else {
      log.severe("Please implement saveFile for " + o.getClass());
      return null;
    }
  }

  /**
   * @return the current {@link #tabbedPane}.
   */
  public JTabbedPane getTabbedPane() {
    return tabbedPane;
  }

  /**
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static Class[] getAvailableReaders() {
    
    // Try first with reflections. Allows more flexibility.
    /*
     * With reflection, we also incloude the "SNP / GWAS" and
     * "GenericGene" readers. We might include them in later releases
     * but for now, we should remove them.
    Class[] available_formats = null;
    try {
      available_formats = Reflect.getAllClassesInPackage("de.zbit.io", true, true, NameAndSignalReader.class,null,true);
    } catch (Throwable t) {
      available_formats = null;
    }
    if (available_formats!=null && available_formats.length>0) {
      return available_formats;
    }
    
    */
    
    // If it is a webstart application, then reflections don't
    // work => include a static list as fallback.
    return new Class[]{mRNAReader.class, miRNAReader.class, DNAmethylationReader.class, ProteinModificationReader.class};
  }

  /**
   * @return all (unique) labels of open tabs.
   */
  public Collection<String> getTabNames() {
    Set<String> titles = new HashSet<String>();
    for (int i=0; i<getTabbedPane().getTabCount(); i++) {
      titles.add(getTabbedPane().getTitleAt(i));
    }
    
    return titles;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#saveFileAs()
   */
  @Override
  public File saveFileAs() {
    return saveFile();
  }
  
}

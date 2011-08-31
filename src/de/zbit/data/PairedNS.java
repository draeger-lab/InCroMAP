/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.zbit.analysis.miRNA2mRNA_pair;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.gui.IntegratorUITools;
import de.zbit.util.ValueTriplet;

/**
 * An implementation of two paired {@link NameAndSignals}.
 * I.e. two {@link NameAndSignals} that match, e.g., to the same gene.
 * 
 * @author Clemens Wrzodek
 */
public class PairedNS<T1 extends NameAndSignals, T2 extends NameAndSignals> extends NameAndSignals implements GeneID {
  private static final long serialVersionUID = 8900546997316139804L;

  /**
   * The first {@link NameAndSignals}
   */
  private T1 ns1;
  
  /**
   * The second {@link NameAndSignals}, matching the first
   * {@link #ns1}.
   */
  private T2 ns2;
  
  /**
   * Pair any matching {@link NameAndSignals}.
   * @param ns1
   * @param ns2
   */
  public PairedNS(T1 ns1, T2 ns2) {
    super(null);
    this.ns1 = ns1;
    this.ns2 = ns2;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getName()
   */
  @Override
  public String getName() {
    // Simply concatenate both names
    StringBuilder out = new StringBuilder(32);
    out.append("[");
    out.append(ns1.getName());
    out.append(implodeString);
    out.append(ns2.getName());
    out.append("]");
    
    return out.toString();
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#merge(java.util.Collection, de.zbit.data.NameAndSignals, de.zbit.data.Signal.MergeType)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source,
    T target, MergeType m) {
    Collection<T1> nsOnes = new ArrayList<T1>(source.size());
    Collection<T2> nsTwos = new ArrayList<T2>(source.size());
    for (T s: source) {
      nsOnes.add((T1) ((PairedNS)s).ns1);
      nsTwos.add((T2) ((PairedNS)s).ns2);
    }
    ((PairedNS)target).ns1 = merge(nsOnes, m);
    ((PairedNS)target).ns2 = merge(nsTwos, m);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getUniqueLabel()
   */
  @Override
  public String getUniqueLabel() {
    // Simply concatenate both names
    StringBuffer out = new StringBuffer();
    out.append(ns1.getUniqueLabel());
    out.append(implodeString);
    out.append(ns2.getUniqueLabel());
    
    return out.toString();
  }
  
  

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnCount()
   */
  public int getColumnCount() {
    int c = ns1.getColumnCount();
    c+=getNumberOfSignals(); //Signals
    c+=getNumberOfAdditionalData(); //Additional data
    c+=ns2.getColumnCount();
    return c;
  }
  
  /**
   * @return a list of columns whose left borders should be drawn
   * in bold to separate the NS pairs.
   */
  public Set<Integer> getBoldBorders() {
    Set<Integer> ret = new HashSet<Integer>();
    
    int c = ns1.getColumnCount();
    if (ns1 instanceof PairedNS) {
      ret.addAll(((PairedNS<?,?>) ns1).getBoldBorders());
    }
    ret.add(c);
    c+=getNumberOfSignals(); //Signals
    c+=getNumberOfAdditionalData(); //Additional data
    ret.add(c);
    
    if (ns2 instanceof PairedNS) {
      for (Integer i: ((PairedNS<?,?>) ns2).getBoldBorders()) {
        ret.add(i+c); 
      }
    }

    return ret;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnName(int)
   */
  public String getColumnName(int columnIndex) {
    return getColumnName(columnIndex, null);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getColumnName(int, java.lang.String[])
   */
  public String getColumnName(int columnIndex, String[] extensionNames) {
    // NS1 Headers
    int ns1ColCount = ns1.getColumnCount();
    if (columnIndex<ns1ColCount) {
      return createTwoLinedHeader(ns1, columnIndex);
    }
    
    // Own Headers
    int supColCount = super.getColumnCount();
    if (columnIndex-ns1ColCount+1<(supColCount))
      return super.getColumnName(columnIndex-ns1ColCount+1, extensionNames);
    
    // NS2 Headers
    return createTwoLinedHeader(ns2, columnIndex-ns1ColCount+1-supColCount);
  }

  /**
   * Create a two-lined header with data type in first column and
   * actual column description in the second.
   * @param ns
   * @param columnIndex
   * @return
   */
  public String createTwoLinedHeader(NameAndSignals ns, int columnIndex) {
    if (ns instanceof PairedNS) {
      // Recursive assign names
      return ns.getColumnName(columnIndex);
    } else {
      return getTypeName(ns.getClass()) + '\n' + ns.getColumnName(columnIndex);
    }
  }

  /**
   * @param class1
   * @return data type name
   */
  public static String getTypeName(Class<? extends NameAndSignals> class1) {
    if (class1.equals(ProteinModificationExpression.class)) {
      return "Prot. Mod.";
    } else if (class1.equals(EnrichmentObject.class)) {
      return "Enriched";
    } else if (class1.equals(PairedNS.class)) {
      return "Other pair";
    } 
    
    return class1.getSimpleName();
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getObjectAtColumn(int, java.lang.Object[])
   */
  @Override
  public Object getObjectAtColumn(int columnIndex, Object[] extensions) {
    int ns1ColCount = ns1.getColumnCount();
    if (columnIndex<ns1ColCount) return ns1.getObjectAtColumn(columnIndex);
    
    int supColCount = super.getColumnCount();
    if (columnIndex-ns1ColCount+1<(super.getColumnCount()))
      return super.getObjectAtColumn(columnIndex-ns1ColCount+1, extensions);
    
    return ns2.getObjectAtColumn(columnIndex-ns1ColCount+1-supColCount);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#setGeneID(int)
   */
  @Override
  public void setGeneID(int geneID) {
    if (ns1 instanceof GeneID) {
      ((GeneID)ns1).setGeneID(geneID);
    }
    if (ns2 instanceof GeneID) {
      ((GeneID)ns2).setGeneID(geneID);
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#getGeneID()
   */
  @Override
  public int getGeneID() {
    // Both ids are supposed to match, if a PairedNS instance is created!
    int id1 = default_geneID;
    int id2 = default_geneID;
    if (ns1 instanceof GeneID) {
      id1 = ((GeneID)ns1).getGeneID();
    }
    if (ns2 instanceof GeneID) {
      id2 = ((GeneID)ns2).getGeneID();
    }
    
    /* XXX:
     * - if only one miRNA then it is always ns1.
     * - if exactly one miRNA, then other is the TARGET,
     * thus return the miRNA of the target!
     */
    if (ns1 instanceof miRNA &&
        !(ns2 instanceof miRNA)) {
      id1 = default_geneID;
    }
    
    // Evaluate...
    if (id2==id1) return id1;
    if (id1==default_geneID) return id2;
    if (id2==default_geneID) return id1;
    if (id2!=id1) return default_geneID;
    
    return default_geneID;
  }
  
  /**
   * Pair two {@link NameAndSignals} lists.
   * @param <T1> any {@link NameAndSignals}
   * @param <T2> any {@link NameAndSignals}
   * @param nsOnes first list to pair with
   * @param nsTwos second list
   * @param geneCenter if true, lists will be gene-centered before pairing
   * (with {@link IntegratorUITools#getMergeTypeSilent()}).
   * @return if exactly one of <code>nsOnes</code> or <code>nsTwos</code> 
   * is an instance of {@link miRNA}, returns a <code>List&lt;PairedNS&lt;miRNA, Other&gt;&gt;</code>
   * else, a <code>List&lt;PairedNS&lt;T1, T2&gt;&gt;</code> is returned.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T1 extends NameAndSignals, T2 extends NameAndSignals> List pair(
    Collection<T1> nsOnes, Collection<T2> nsTwos, boolean geneCenter) {
    
    
    // miRNAs should be paired with OTHERS by target
    // If XOR one of both is miRNA then pair by target.
    // See also {@link PairData#calculateMergedSignal(List, MergedSignalDialog)}
    if (NameAndSignals.isMicroRNA(nsOnes)) {
      if (!(NameAndSignals.isMicroRNA(nsTwos))) {
        return pairByTarget((Collection<miRNA>)nsOnes, nsTwos, geneCenter);
      }
    } else if (NameAndSignals.isMicroRNA(nsTwos)) {
      return pairByTarget((Collection<miRNA>)nsTwos, nsOnes, geneCenter);
    }
    
    // Perform pairing by Identifier (geneID or name, if both are miRNAs).
    if (geneCenter) {
      MergeType mt = IntegratorUITools.getMergeTypeSilent();
      nsOnes = NameAndSignals.geneCentered(nsOnes, mt);
      nsTwos = NameAndSignals.geneCentered(nsTwos, mt);
    }
    return pairByGeneID(nsOnes, nsTwos);
  }
  
  public static <T1 extends NameAndSignals, T2 extends NameAndSignals> List<PairedNS<T1, T2>> pairByGeneID(
    Collection<T1> nsOnes, Collection<T2> nsTwos) {
    List<PairedNS<T1, T2>> ret = new ArrayList<PairedNS<T1, T2>>(nsOnes.size());
    
    // Map from geneID to listOfNs
    Map<Object, List<T1>> map1 = getNSIdentifierToNSmap(nsOnes);
    Map<Object, List<T2>> map2 = getNSIdentifierToNSmap(nsTwos);
    
    // Add all matching pairs
    for (Entry<Object, List<T1>> e1 : map1.entrySet()) {
      if (e1.getKey().equals(default_geneID)) continue; // These pairs do not make sense.
      List<T2> v2 = map2.get(e1.getKey());
      if (v2==null || v2.size()<1) continue; // Only add pairs
      
      // ... this is why input data should be gene, and not probe-centered.
      for (T1 t1 : e1.getValue()) {
        for (T2 t2 : v2) {
          ret.add(new PairedNS<T1, T2>(t1, t2));
        }
      }
      
    }
    
    return ret;
  }
  
  /**
   * 
   * @param <T1>
   * @param <T2>
   * @param nsOnes
   * @param nsTwos
   * @param geneCenter if true, lists will be gene-centered before pairing
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T1 extends miRNA, T2 extends NameAndSignals> List<PairedNS<miRNA, T2>> pairByTarget(
    Collection<T1> nsOnes, Collection<T2> nsTwos, boolean geneCenter) {
    
    // Create map from gene id to NS
    Map<Integer, Collection<T2>> geneId2NS = miRNA2mRNA_pair.getGeneID2mRNAMapping(nsTwos);
    

    // miRNA 2 mRNA pairing
    List<ValueTriplet<miRNA, miRNAtarget, T2>> pairs = miRNA2mRNA_pair.getExpressionPairedTable((Collection<miRNA>)nsOnes, geneCenter, IntegratorUITools.getMergeTypeSilent(), geneId2NS);

    // Create PairedNS instances
    List<PairedNS<miRNA, T2>> ret = new ArrayList<PairedNS<miRNA, T2>>(nsOnes.size());
    for (ValueTriplet<miRNA, miRNAtarget, T2> pair : pairs) {
      
      PairedNS<miRNA, T2> ns = new PairedNS<miRNA, T2>(pair.getA(), pair.getC());
      miRNAtarget relation = pair.getB();
      
      ns.addData(miRNAtarget.SOURCE_KEY, relation.getSource());
      ns.addData(miRNAtarget.PVAL_KEY, relation.isExperimental()?"experiment":relation.getPValue());
      
      
      ret.add(ns);
    }
    
    return ret;
  }

  /**
   * @return
   */
  public T1 getNS1() {
    return ns1;
  }
  
  /**
   * @return
   */
  public T2 getNS2() {
    return ns2;
  }
  
  /**
   * Create a list of all {@link #getNS1()} items.
   * @param <T>
   * @param pairs
   * @return
   */
  public static <T extends NameAndSignals> List<T> getNS1AsList(Collection<PairedNS<T, ?>> pairs) {
    // Ensure that we have a list
    final List<PairedNS<T,?>> list;
    if (pairs instanceof List) list = (List<PairedNS<T,?>>) pairs;
    else list = new ArrayList<PairedNS<T,?>>(pairs);
    
    // Create a custom wrapper
    return new AbstractList<T>() {
      @Override
      public T get(int index) {
        return list.get(index).getNS1();
      }

      @Override
      public int size() {
        return list.size();
      }
    };
  }
  
  /**
   * Create a list of all {@link #getNS2()} items.
   * @param <T>
   * @param pairs
   * @return
   */
  public static <T extends NameAndSignals> List<T> getNS2AsList(Collection<PairedNS<?, T>> pairs) {
    // Ensure that we have a list
    final List<PairedNS<?, T>> list;
    if (pairs instanceof List) list = (List<PairedNS<?, T>>) pairs;
    else list = new ArrayList<PairedNS<?, T>>(pairs);
    
    // Create a custom wrapper
    return new AbstractList<T>() {
      @Override
      public T get(int index) {
        return list.get(index).getNS2();
      }

      @Override
      public int size() {
        return list.size();
      }
    };
  }

}

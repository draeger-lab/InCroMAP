/*
 * %W% %E%
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer. 
 *   
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution. 
 *   
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.  
 * 
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,   
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER  
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS 
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.tree.TreeNode;
import javax.swing.treetable.AbstractTreeTableModel;
import javax.swing.treetable.TreeTableModel;

/**
 * FileSystemModel is a TreeTableModel representing a hierarchical file 
 * system. Nodes in the FileSystemModel are FileNodes which, when they 
 * are directory nodes, cache their children to avoid repeatedly querying 
 * the real file system. 
 * 
 * @version %I% %G%
 *
 * @author Philip Milne
 * @author Scott Violet
 */

public class FileSystemModel extends AbstractTreeTableModel 
                             implements TreeTableModel {

    // Names of the columns.
    static protected String[]  cNames = {"Name", "Size", "Type", "Modified"};

    // Types of the columns.
    static protected Class[]  cTypes = {TreeTableModel.class, Integer.class, String.class, Date.class};

    // The the returned file length for directories. 
    public static final Integer ZERO = new Integer(0); 

    public FileSystemModel() { 
	super(new FileNode(new File(File.separator))); 
    }

    //
    // Some convenience methods. 
    //

    protected File getFile(Object node) {
	FileNode fileNode = ((FileNode)node); 
	return fileNode.getFile();       
    }

    protected Object[] getChildren(Object node) {
	FileNode fileNode = ((FileNode)node); 
	return fileNode.getChildren(); 
    }

    //
    // The TreeModel interface
    //

    public int getChildCount(Object node) { 
	Object[] children = getChildren(node); 
	return (children == null) ? 0 : children.length;
    }

    public Object getChild(Object node, int i) { 
	return getChildren(node)[i]; 
    }

    // The superclass's implementation would work, but this is more efficient. 
    public boolean isLeaf(Object node) { return getFile(node).isFile(); }

    //
    //  The TreeTableNode interface. 
    //

    public int getColumnCount() {
	return cNames.length;
    }

    public String getColumnName(int column) {
	return cNames[column];
    }

    public Class getColumnClass(int column) {
	return cTypes[column];
    }
 
    public Object getValueAt(Object node, int column) {
	File file = getFile(node); 
	try {
	    switch(column) {
	    case 0:
		return file.getName();
	    case 1:
		return file.isFile() ? new Integer((int)file.length()) : ZERO;
	    case 2:
		return file.isFile() ?  "File" : "Directory";
	    case 3:
		return new Date(file.lastModified());
	    }
	}
	catch  (SecurityException se) { }
   
	return null; 
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#iterator()
     */
    @Override
    public Iterator iterator() {
      // TODO Auto-generated method stub
      return null;
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public int size() {
      // TODO Auto-generated method stub
      return 0;
    }
}

/* A FileNode is a derivative of the File class - though we delegate to 
 * the File object rather than subclassing it. It is used to maintain a 
 * cache of a directory's children and therefore avoid repeated access 
 * to the underlying file system during rendering. 
 */
class FileNode implements TreeNode { 
    File     file; 
    Object[] children; 

    public FileNode(File file) { 
	this.file = file; 
    }

    // Used to sort the file names.
    static private MergeSort  fileMS = new MergeSort() {
	public int compareElementsAt(int a, int b) {
	    return ((String)toSort[a]).compareTo((String)toSort[b]);
	}
    };

    /**
     * Returns the the string to be used to display this leaf in the JTree.
     */
    public String toString() { 
	return file.getName() + " TOSTRING";
    }

    public File getFile() {
	return file; 
    }

    /**
     * Loads the children, caching the results in the children ivar.
     */
    protected Object[] getChildren() {
	if (children != null) {
	    return children; 
	}
	try {
	    String[] files = file.list();
	    if(files != null) {
		fileMS.sort(files); 
		children = new FileNode[files.length]; 
		String path = file.getPath();
		for(int i = 0; i < files.length; i++) {
		    File childFile = new File(path, files[i]); 
		    children[i] = new FileNode(childFile);
		}
	    }
	} catch (SecurityException se) {}
	return children; 
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getChildAt(int)
     */
    @Override
    public TreeNode getChildAt(int childIndex) {
      // TODO Auto-generated method stub
      return null;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    @Override
    public int getChildCount() {
      // TODO Auto-generated method stub
      return 0;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getParent()
     */
    @Override
    public TreeNode getParent() {
      // TODO Auto-generated method stub
      return null;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
     */
    @Override
    public int getIndex(TreeNode node) {
      // TODO Auto-generated method stub
      return 0;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getAllowsChildren()
     */
    @Override
    public boolean getAllowsChildren() {
      // TODO Auto-generated method stub
      return false;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#isLeaf()
     */
    @Override
    public boolean isLeaf() {
      // TODO Auto-generated method stub
      return false;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#children()
     */
    @Override
    public Enumeration children() {
      // TODO Auto-generated method stub
      return null;
    }
}



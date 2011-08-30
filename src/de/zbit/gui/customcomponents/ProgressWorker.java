/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui.customcomponents;

import javax.swing.SwingWorker;

import de.zbit.util.AbstractProgressBar;

/**
 * A simple {@link SwingWorker} extension with a {@link AbstractProgressBar}.
 * @author Clemens Wrzodek
 */
public abstract class ProgressWorker<T, V> extends SwingWorker<T, V>{
  AbstractProgressBar progress=null;
  
  public void setProgressBar(AbstractProgressBar progress) {
    this.progress = progress;
  }
  
}

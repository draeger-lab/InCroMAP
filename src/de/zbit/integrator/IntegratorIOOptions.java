package de.zbit.integrator;

import java.util.Arrays;

import de.zbit.io.NameAndSignalReader;
import de.zbit.util.Reflect;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;

/**
 * IO options for Integrator.
 * @author Clemens Wrzodek
 */
public interface IntegratorIOOptions extends de.zbit.util.prefs.KeyProvider {
  static Class[] available_formats = Reflect.getAllClassesInPackage("de.zbit.io", true, true, NameAndSignalReader.class,null,true);
  
  /**
   * Readable file formats
   */
  public static final Option<Class> READER = new Option<Class>("Data type",
      Class.class, "Readable input data types",
      new Range<Class>(Class.class, Arrays.asList(available_formats)),
      (short) 2, "-t", available_formats[0]);
  
}

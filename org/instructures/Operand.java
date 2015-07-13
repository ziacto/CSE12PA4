////////////////////////////////////////////////////////////////////////////////
// Operand.java
////////////////////////////////////////////////////////////////////////////////

package org.instructures;

import java.io.File;
import java.util.*;

// An `Operand` is a value specified on the command line as a string
// which represents either a file name (java.io.File), an integer, or
// a general string as a catch-all.
//
// Operands can be standalone (as with some file arguments to
// commands), or associated with a particular Option.
public class Operand<T>
{
  private static final String DEFAULT_NAME = "ARG";
  private static final Set<Class<?>> acceptedTypes = new HashSet<>(
    Arrays.asList(File.class, String.class, Integer.class));

  // Factory to make a new Operand instance to capture bindings of the
  // given `operandType`.
  public static <T> Operand<T> create(Class<T> operandType) {
    return create(operandType, null);
  }
  
  // Factory to make a new Operand instance to capture bindings of the
  // given `operandType`, represented in the usage message with the
  // given `docName` string.
  public static <T> Operand<T> create(Class<T> operandType, String docName) {
    if (!acceptedTypes.contains(operandType)) {
      unsupportedType(operandType);
    }
    return new Operand<>(operandType, docName);
  }

  // Converts the given raw, uninterpreted string from the command line into
  // an instance of the Operand's type.
  public T convertArgument(String rawFormat) {
    Object result = null;
    if (operandType == String.class) {
      result = rawFormat;
    }
    else if (operandType == Integer.class) {
      result = Integer.parseInt(rawFormat);
    }
    else if (operandType == File.class) {
      result = new File(rawFormat);
    }
    else {
      unsupportedType(operandType);
    }
    return operandType.cast(result);
  }

  public boolean hasDefaultValue() {
    return hasDefaultValue;
  }

  public T getDefaultValue() {
    return defaultValue;
  }
  
  public Operand<T> setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
    this.hasDefaultValue = true;
    return this;
  }

  public String getDocName() {
    return docName;
  }

  public String toString() {
    return String.format("%s [a %s]", docName, operandType);
  }
  
  private final Class<T> operandType;
  private final String docName;
  private boolean hasDefaultValue = false;
  private T defaultValue = null;

  protected Operand(Class<T> operandType, String docName) {
    this.operandType = operandType;
    this.docName = (docName != null) ? docName : DEFAULT_NAME;
  }

  private static void unsupportedType(Class<?> operandType) {
    throw new IllegalArgumentException(
      String.format("Type %s is not supported", operandType));
  }
}

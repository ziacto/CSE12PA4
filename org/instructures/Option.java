////////////////////////////////////////////////////////////////////////////////
// Option.java
////////////////////////////////////////////////////////////////////////////////

package org.instructures;

import java.util.*;

public class Option
{
  private static final String SHORT_FLAG_NAME_REGEX = "[A-Za-z0-9]";
  private static final String LONG_FLAG_NAME_REGEX = "[A-Za-z0-9_-]+";
    
  // Factory for making simple boolean options.
  public static Option create(String flags) {
    return new Option(flags, null);
  }

  // Factory for making options that take an operand.
  public static Option create(String flags, Operand<?> operand) {
    return new Option(flags, operand);
  }

  // Uses the given `docSummary` string to describe this option in a
  // generated help message.
  public Option summary(String docSummary) {
    this.summary = docSummary;
    return this;
  }

  // Creates a one way dependency on the given `dependency` option. If the
  // current option is present but the `dependency` option is missing, then
  // an error will be reported to the user.
  public Option associatedWith(Option dependency) {
    dependencies.add(dependency);
    return this;
  }

  // Parses the flag string given at construction time and adds all
  // long-form flags to the given `longFlagsOut` collection and all
  // short-form flags to the given `shortFlagsOut` collection.
  //
  // Single-character flags start with a single "-" and must be a
  // letter or digit. Long-form flags start with a "--" and must be
  // composed of letters, digits, hyphens, and underscores. Throws an
  // IllegalArgumentException if the a flag specified for this Option
  // is malformed.
  public void getFlags(Collection<String> longFlagsOut,
                       Collection<String> shortFlagsOut) {
    String[] flags = flagsStr.split("[, ]+");
    for (String flag: flags) {
      if (flag.startsWith("--")) {
        String longName = flag.substring(2);
        if (longName.matches(LONG_FLAG_NAME_REGEX)) {
          longFlagsOut.add(longName);
          continue;
        }
      } else if (flag.startsWith("-")) {
        String shortName = flag.substring(1);
        if (shortName.matches(SHORT_FLAG_NAME_REGEX)) {
          shortFlagsOut.add(shortName);
          continue;
        }
      }
      throw new IllegalArgumentException(
        String.format("Invalid flag syntax: \"%s\"", flag));
    }
  }

  public String getSummary() {
    return summary;
  }

  public Collection<Option> getDependencies() {
    return dependencies;
  }

  public boolean hasOperand() {
    return (operand != null);
  }

  public Operand<?> getOperand() {
    return operand;
  }

  public String toString() {
    if (hasOperand()) {
      return String.format("<%s> [operand: %s]", flagsStr, operand);
    }
    else {
      return String.format("<%s>", flagsStr);
    }
  }
  
  private final String flagsStr;
  private final Operand<?> operand;
  private final List<Option> dependencies = new ArrayList<>();
  private String summary = "";
  
  private Option(String flagsStr, Operand<?> operand) {
    this.flagsStr = flagsStr;
    this.operand = operand;
  }
}

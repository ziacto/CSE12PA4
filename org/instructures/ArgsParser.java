////////////////////////////////////////////////////////////////////////////////
// ArgsParser.java
////////////////////////////////////////////////////////////////////////////////

package org.instructures;

import java.io.*;
import java.util.*;

// A general command-line argument parser following the Unix
// single-character option conventions (similar to getopt,
// http://en.wikipedia.org/wiki/Getopt) and also the GNU long-form
// option conventions (cf. getopt_long, ibid.).
//
// The API uses the fluent-interface style, as discussed in:
// http://www.martinfowler.com/bliki/FluentInterface.html.
public class ArgsParser
{
  // Canned messages and formatting strings.
  private static final String
    DEFAULT_VERSION = "(unknown)",
    HELP_MESSAGE = "display this help and exit",
    VERSION_MESSAGE = "output version information and exit",
    GENERIC_OPTIONS = "OPTIONS",
    OPTION_SUMMARY_FMT = "%4s%s %-20s   %s%n";
  
  // Factory to make a new ArgsParser instance, to generate help
  // messages and to process and validate the arguments for a command
  // with the given `commandName`.
  public static ArgsParser create(String commandName) {
    return new ArgsParser(commandName);
  }

  // A queryable container to hold the parsed results.
  //
  // Options are added using on of the `optional`, `require`, and
  // `requireOneOf` methods. The presence of such Options in the
  // actual arguments processed can be queried via the `hasOption`
  // method.
  //
  // Operands can be associated with an Option or can stand
  // alone. Standalone Operands are added using the `requiredOperand`,
  // `optionalOperand`, `oneOrMoreOperands`, and `zeroOrMoreOperands`
  // methods. Operands associated with an Option are added when that
  // Option is added.
  public class Bindings {
    public boolean hasOption(Option optionToQuery) {
      return options.contains(optionToQuery);
    }

    // If an Operand is optional and has a default value, then this method
    // will return the default value when the Operand wasn't specified.
    public <T> T getOperand(Operand<T> operand) {
      if (operands.containsKey(operand)) {
        List<T> result = getOperands(operand);
        if (result.size() == 1) {
          return result.get(0);
        }
      }
      else if (operand.hasDefaultValue()) {
        return operand.getDefaultValue();
      }
      throw new RuntimeException(
        String.format("Expected one binding for operand %s", operand));
    }
    
    public <T> List<T> getOperands(Operand<T> operand) {
      List<T> result = new ArrayList<>();
      if (operands.containsKey(operand)) {
        List<String> uninterpretedStrings = operands.get(operand);
        for (String stringFormat: uninterpretedStrings) {
          result.add(operand.convertArgument(stringFormat));
        }
      }
      return result;
    }

    private void addOption(Option option) {
      options.add(option);
    }
    
    private void bindOperand(Operand<?> operand, String lexeme) {
      List<String> bindings;
      if (operands.containsKey(operand)) {
        bindings = operands.get(operand);
      }
      else {
        bindings = new ArrayList<>();
        operands.put(operand, bindings);
      }
      try {
        operand.convertArgument(lexeme);
      }
      catch (Exception e) {
        throw new RuntimeException(
          String.format("(invalid format) %s", e.getMessage()));
      }
      bindings.add(lexeme);
    }

    private final Set<Option> options = new HashSet<>();
    private final Map<Operand, List<String>> operands = new HashMap<>();
    
    private Bindings() {
      /* intentionally left blank */
    }
  }

  // Parses the given command-line argument values according to the
  // specifications set through calls to the `optional`, `require`,
  // `requireOneOf` and `operands` methods.
  //
  // When the given arguments don't match the options specified, an
  // error message is printed and the program exits.
  //
  // Options for displaying the help message and the version message
  // are supported by calls made to `help` and `version`,
  // respectively. A call to 'parse` will cause the program to exit if
  // the help or version options are present in the given `args`. If
  // both are specified, then both will be printed before exit.
  public ArgsParser.Bindings parse(String[] args) {
    Bindings bindings = new Bindings();
    // TODO

    Queue<Operand> operandQueue = new LinkedList<>(operandSeq);
    List<Option> allOpts = new ArrayList<>();
    UsageTable usageTable = new UsageTable();

    for (int i=0 ; i<args.length ; ++i) {
      String arg = args[i];
      String lexeme = null;

      if (arg.startsWith("--")) { // Long Flag Case
        String flag = arg.substring(2);
        if (arg.contains("=")) {
          flag = arg.substring(2, arg.indexOf("="));
        }

        if (!longFlagLookup.containsKey(flag)) {
          usageTable.error("Unrecognized Option: %s%n", flag);
        } else {
          Option opt = longFlagLookup.get(flag);
          bindings.addOption(opt);
          allOpts.add(opt);

          if (opt.hasOperand()) {
            Operand op = opt.getOperand();

            if (arg.contains("=")) {
              lexeme = arg.substring(arg.indexOf("=") + 1);
              bindings.bindOperand(op, lexeme);
            } else if (i+1 < args.length) {
              lexeme = args[i+1];
              ++i;
              if (op != null && lexeme != null) {
                bindings.bindOperand(op, lexeme);
              }
            } else {
              usageTable.error("Missing Operand: %s%n", op.toString());
            }
          }
        }
      } else if (arg.startsWith("-") && arg.length() == 2) { // Short Flag Case
        String flag = arg.substring(1);

        if (!shortFlagLookup.containsKey(flag)) {
          usageTable.error("Error Unrecognized Option: %s%n", flag);
        } else {
          Option opt = shortFlagLookup.get(flag);
          bindings.addOption(opt);
          allOpts.add(opt);

          if (opt.hasOperand()) {
            Operand op = opt.getOperand();

            if (i+1 < args.length) {
              lexeme = args[i+1];
              ++i;
              if (op != null && lexeme != null) {
                bindings.bindOperand(op, lexeme);
              }
            } else {
              usageTable.error("Error: Missing Operand: %s%n", op.toString());
            }
          }
        }
      } else if (arg.startsWith("-") && arg.length() > 2) {
        String flag = arg.substring(1, 2);

        if (!shortFlagLookup.containsKey(flag)) {
          usageTable.error("Error Unrecognized Option: %s%n", flag);
        } else {
          Option opt = shortFlagLookup.get(flag);
          bindings.addOption(opt);
          allOpts.add(opt);

          if (opt.hasOperand()) {
            Operand op = opt.getOperand();

            if (arg.contains("=")) {
              lexeme = arg.substring(arg.indexOf("="));
              bindings.bindOperand(op, lexeme);
            } else {
              lexeme = arg.substring(2);
              bindings.bindOperand(op, lexeme);
            }
          } else {
            String str = arg;
            while (str.length() > 0) {
              flag = str.substring(1, 2);
              if (!shortFlagLookup.containsKey(flag)) {
                usageTable.error("Error Unrecognized Option: %s%n", flag);
              } else {
                opt = shortFlagLookup.get(flag);
                bindings.addOption(opt);
                allOpts.add(opt);
              }
              str = str.substring(1);
            }
          }
        }
      } else {
        Operand<?> nextOperand = operandQueue.peek();
        lexeme = args[i];

        if (nextOperand == null) {
          usageTable.error("Error: Extra Operand: %s%n", lexeme);
        }
        if (!allowsMult(nextOperand)) {
          operandQueue.remove();
        }
        bindings.bindOperand(nextOperand, lexeme);
      }
    }

    if (bindings.hasOption(this.helpOption)) {
      usageTable.printUsage();
      System.exit(0);
    }

    for (RequiredMode reqMode : requiredModes) {
      reqMode.followsRule(allOpts);
    }

    while (!operandQueue.isEmpty()) {
      Operand<?> op = operandQueue.remove();
      if (!isOptional(op)) {
        try {
          if (allowsMult(op)) {
            if (bindings.getOperands(op).toString().length() < 1) {
              usageTable.error("Error: %s operand required%n", op.toString());
            }
          } else if (bindings.getOperand(op).toString().length() < 1) {
            usageTable.error("Error: %s operand required%n", op.toString());
          }
        } catch (Exception e) {
          usageTable.error("Error: %s operand required%n", op.toString());
        }
      }
    }

    return bindings;
  }

  // Uses the given `summaryString` when the help/usage message is printed.
  public ArgsParser summary(String summaryString) {
    this.summaryString = summaryString;
    return this;
  }

  // Enables the command to have an option to display the current
  // version, represented by the given `versionString`. The version
  // option is invoked whenever any of the given `flags` are used,
  // where `flags` is a comma-separated list of valid short- and
  // long-form flags.
  public ArgsParser versionNameAndFlags(String versionString, String flags) {
    this.versionString = versionString;
    this.versionOption = Option.create(flags).summary(VERSION_MESSAGE);
    return optional(versionOption);
  }

  // Enables an automated help message, generated from the options
  // specified.  The help message will be invoked whenever any of the
  // given `flags` are used.
  //
  // The `flags` parameter is a comma-separated list of valid short-
  // and long-form flags, including the leading `-` and `--` marks.
  public ArgsParser helpFlags(String flags) {
    this.helpOption = Option.create(flags).summary(HELP_MESSAGE);
    return optional(helpOption);
  }

  // Adds the given option to the parsing sequence as an optional
  // option. If the option takes an Operand, the value of the
  // associated operand can be accessed using a reference to that
  // specific Operand instance.
  //
  // Throws an IllegalArgumentException if the given option specifies
  // flags that have already been added.

  List<Option> optionalOptionsList = new ArrayList<>();

  public ArgsParser optional(Option optionalOption) {
    // TODO
    if (optionalOptionsList.contains(optionalOption)) {
      throw new IllegalArgumentException();
    } else {
      optionalOptionsList.add(optionalOption);
      populateFlags(optionalOption);
      allOptions.add(optionalOption);
    }
    return this;
  }

  // Adds the given option to the parsing sequence as a required
  // option. If the option is not present during argument parsing, an
  // error message is generated using the given `errString`. If the
  // option takes an Operand, the value of the associated operand can
  // be accessed using a reference to that specific Operand instance.
  //
  // Throws an IllegalArgumentException if the given option specifies
  // flags that have already been added.
  public ArgsParser require(String errString, Option requiredOption) {
    populateFlags(requiredOption);
    allOptions.add(requiredOption);
    return requireOneOf(errString, requiredOption);
  }

  // Adds the given set of mutually-exclusive options to the parsing
  // sequence. An error message is generated using the given
  // `errString` when multiple options that are mutually exclusive
  // appear, and when none appear. An example of such a group of
  // mutually- exclusive options is when the option specifies a
  // particular mode for the command where none of the modes are
  // considered as a default.
  //
  // Throws an IllegalArgumentException if any of the given options
  // specify flags that have already been added.

  public ArgsParser requireOneOf(String errString, Option... exclusiveOptions) {
    for (Option option : exclusiveOptions) {
      allOptions.add(option);
      populateFlags(option);
    }
    RequiredMode modeSpec = new RequiredMode(exclusiveOptions, errString);
    requiredModes.add(modeSpec);
    return this;
  }

  // Adds the given operand to the parsing sequence as a required
  // operand to appear exactly once. The matched argument's value is
  // retrievable from the `ArgsParser.Bindings` store by passing the
  // same `requiredOperand` instance to the `getOperand` method.
  public ArgsParser requiredOperand(Operand requiredOperand) {
    handleOperand(requiredOperand, false, false);
    return this;
  }

  // Adds the given operand to the parsing sequence as an optional
  // operand. The matched argument's value is retrievable from the
  // `ArgsParser.Bindings` store by passing the same `optionalOperand`
  // instance to the `getOperands` method, which will return either a
  // the empty list or a list with a single element.
  public ArgsParser optionalOperand(Operand optionalOperand) {
    handleOperand(optionalOperand, true, false);
    return this;
  }

  // Adds the given operand to the parsing sequence as a required
  // operand that must be specifed at least once and can be used
  // multiple times (the canonical example would be a list of one or
  // more input files).
  //
  // The values of the arguments matched is retrievable from the
  // `ArgsParser.Bindings` store by passing the same `operand`
  // instance to the `getOperands` method, which will return a list
  // with at least one element (should the arguments pass the
  // validation process).
  public ArgsParser oneOrMoreOperands(Operand operand) {
    handleOperand(operand, false, true);
    return this;
  }

  // Adds the given operand to the parsing sequence as an optional
  // operand that can be used zero or more times (the canonical
  // example would be a list of input files, where if none are given
  // then stardard input is assumed).
  //
  // The values of the arguments matched is retrievable from the
  // `ArgsParser.Bindings` store by passing the same `operand`
  // instance to the `getOperands` method, which will return a list of
  // all matches, potentially the empty list.
  public ArgsParser zeroOrMoreOperands(Operand operand) {
    handleOperand(operand, true, true);
    return this;
  }

  private final String commandName;

  private String summaryString = null;
  private String versionString = DEFAULT_VERSION;
  private Option helpOption = null;
  private Option versionOption = null;
  
  private ArgsParser(String commandName) {
    this.commandName = commandName;
  }

  // TODO: Add more code here if you think it'll be helpful!

  private List<Operand> operandSeq = new ArrayList<>();
  private Map<Operand, String> operandUsage = new HashMap<>();

  private void handleOperand (Operand op, boolean optional, boolean allowMult) {
    operandSeq.add(op);
    String usageText = op.getDocName();
    if (allowMult) {
      usageText = String.format("%s...", usageText);
    }
    if (optional) {
      usageText = String.format("[%s]", usageText);
    }
    operandUsage.put(op, usageText);
  }

  private boolean isOptional (Operand op) {
    return operandUsage.get(op).startsWith("[");
  }

  private boolean allowsMult (Operand op) {
    return operandUsage.get(op).contains("...");
  }

  Map<String, Option> longFlagLookup = new HashMap<>();
  Map<String, Option> shortFlagLookup = new HashMap<>();
  List<Option> allOptions = new ArrayList<>();

  private void populateFlags (Option o) {
    List<String> shortFlags = new ArrayList<>();
    List<String> longFlags = new ArrayList<>();

    o.getFlags(longFlags, shortFlags);

    for (String str : longFlags) {
      longFlagLookup.put(str, o);
    }
    for (String str : shortFlags) {
      shortFlagLookup.put(str, o);
    }
  }


  private List<RequiredMode> requiredModes = new ArrayList<>();
  class RequiredMode {
    private Set<Option> modes = new HashSet<Option>();
    private String errString = null;
    UsageTable usageTable = new UsageTable();

    RequiredMode (Option[] options, String str) {
      for (Option option : options) {
        modes.add(option);
      }

      errString = str;
    }

    boolean followsRule (List<Option> actualParsedOptions) {
      boolean result = true;
      Option mode = null;

      for (Option actual : actualParsedOptions) {
        if (modes.contains(actual)) {
          if (mode == null) {
            mode = actual;
          } else if (mode != actual) {
            result = false;
            usageTable.error("Error: Mode does not match parsed option%n", "");
          }
        }
      }
      if (mode == null) {
        result = false;
        usageTable.error("Error: mode is null%n", "");
      }
      return false;
    }
  }

  class UsageTable {
    void error (String errStr, String opt) {
      System.err.printf(errStr, opt);
      printUsage();
      System.exit(1);
    }

    void printUsage () {
      System.err.printf("Usage: %s", commandName);

      // Print options
      

      // Print utility-level operands
      for (Operand op : operandSeq) {
        String usageString = operandUsage.get(op);
        System.err.printf(" %s", usageString);
      }
      System.err.printf("%n");
      if (summaryString != null) {
        System.err.printf("%s%n", summaryString);
      }

      // Print Options
      for (Option opt : allOptions) {
        String summary = opt.getSummary();
        List<String> shortFlags = new ArrayList<>();
        List<String> longFlags = new ArrayList<>();
        opt.getFlags(longFlags, shortFlags);

        while (!shortFlags.isEmpty() || !longFlags.isEmpty()) {
          String shortFlag = "";
          if (!shortFlags.isEmpty()) {
            shortFlag = "-" + shortFlags.remove(0);
          }
          String longFlag = "";
          if (!longFlags.isEmpty()) {
            longFlag = "--" + longFlags.remove(0);
          }
          String separator =
            (shortFlag.isEmpty() || longFlag.isEmpty()) ? " " : ",";
          if (opt.hasOperand()) {
            Operand<?> op = opt.getOperand();
            String operandName = op.getDocName();
            if (operandName != null && !operandName.isEmpty()) {
              if (!longFlag.isEmpty()) {
                longFlag += "=" + operandName;
              } else {
                shortFlag += " " + operandName;
              }
            }
          }
          System.err.printf(OPTION_SUMMARY_FMT, shortFlag,
            separator, longFlag, summary);
          summary = "";
        }
      }
    }
  }
}

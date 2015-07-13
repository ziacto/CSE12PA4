import java.util.*;
import java.io.File;
import java.io.IOException;
import org.instructures.*;

public class HuffmanCodes {
  private final Option ENCODE, DECODE;
  private final Option SHOW_FREQUENCY, SHOW_CODES, SHOW_BINARY;
  private final ArgsParser argsParser;
  private final Operand<File> IN, OUT;
  private ArgsParser.Bindings settings;

  public HuffmanCodes () {
    this.ENCODE = Option.create("-e, --encode").summary("Encodes IN to OUT");
    this.DECODE = Option.create("-d, --decode").summary("Decodes IN to OUT");
    this.SHOW_FREQUENCY = Option.create("--show-frequency")
      .associatedWith(ENCODE).summary("Output byte frequencies");
    this.SHOW_CODES = Option.create("--show-codes")
      .summary("Output the code for each byte");
    this.SHOW_BINARY = Option.create("--show-binary").associatedWith(ENCODE)
      .summary("Output a base-two representation of the encoded sequence");
    this.argsParser = ArgsParser.create("java HuffmanCodes")
      .summary("Encodes and decodes files using Huffman's technique")
      .helpFlags("-h, --help");
    this.IN = Operand.create(File.class, "IN");
    this.OUT = Operand.create(File.class, "OUT");
    argsParser.requireOneOf("mode required", ENCODE, DECODE)
      .optional(SHOW_FREQUENCY).optional(SHOW_CODES).optional(SHOW_BINARY)
      .requiredOperand(IN).requiredOperand(OUT);
  }

  static final Map<String, String> escapedLiterals = new HashMap<>();
  static {
    escapedLiterals.put("\n", "\'\\n\'");
    escapedLiterals.put("\r", "\'\\r\'");
    escapedLiterals.put("\\", "\'\\\\\'");
    escapedLiterals.put("\'", "\'\\\'\'");
  }

  public static void main (String[] args) {
    HuffmanCodes app = new HuffmanCodes();
    app.start(args);
  }

  Map<Byte, Integer> freqsMap;
  Map<Byte, String> codeMap;
  String encodedString;

  public void start (String[] args) {
    settings = argsParser.parse(args);
    try (BitInputStream in = new BitInputStream(settings.getOperand(IN));
      BitOutputStream out = new BitOutputStream(settings.getOperand(OUT))) {

      freqsMap = new LinkedHashMap<>();
      codeMap = new LinkedHashMap<>();
      encodedString = "";

      if (settings.hasOption(ENCODE)) {
        encode(in, out);
      } else {
        decode(in, out);
      }

    } catch (Exception e) {
      System.err.printf("Error: %s%n", e.getMessage());
      System.exit(1);
    }
  }

  private void printFreqs (Map<Byte, Integer> freqsMap) {
    freqsMap = sortFreqsMap(freqsMap);
    System.out.printf("FREQUENCY TABLE%n");
    for (Map.Entry<Byte, Integer> entry : freqsMap.entrySet()) {
      char letter = (char) entry.getKey().intValue();
      Integer number = entry.getValue();
      System.out.printf("'%s': %d%n", letter, number);
    }
  }

  private void printCodes (Map<Byte, String> codeMap) {
    System.out.println("CODES");
    for (Map.Entry<Byte, String> entry : codeMap.entrySet()) {
      System.out.printf("\"%s\" -> '%s'%n", entry.getValue(), (char) entry.getKey().intValue());
    }
  }

  private void printBinary (String encodedString) {
    System.out.println("ENCODED SEQUENCE");
    System.out.println(encodedString);
  }

  private void encode (BitInputStream in, BitOutputStream out) {
    try {
      byte[] allBytes = in.allBytes();

      freqsMap = countFrequencies(allBytes);
      freqsMap = sortFreqsMap(freqsMap);

      Node tree = buildTree(freqsMap);
      codeMap = tree.getAllCodes();
      codeMap = sortCodeMap(codeMap);

      encodedString = "";
      for (int i=0; i<allBytes.length; ++i) {
        encodedString += codeMap.get(new Byte(allBytes[i]));
      }

      if (settings.hasOption(SHOW_FREQUENCY)) {
        printFreqs(freqsMap);
      }
      if (settings.hasOption(SHOW_CODES)) {
        printCodes(codeMap);
      }
      if (settings.hasOption(SHOW_BINARY)) {
        printBinary(encodedString);
      }
    
      int inputLengthBits = allBytes.length * 8;
      int inputLengthBytes = (int) Math.ceil((double) inputLengthBits/8);

      int headerLength = 32 + freqsMap.size() * 8 + freqsMap.size()*2-1;
      int encodedLength = encodedString.length();

      out.writeInt(inputLengthBytes);
      tree.writeTo(out);
      for (int i=0; i<encodedLength; ++i) {
        out.writeBit(Integer.parseInt(encodedString.substring(i,i+1)));
      }

      int outputLengthBits = headerLength + encodedLength;
      int outputLengthBytes = (int) Math.ceil((double) outputLengthBits/8);
      double percentChange = ((double) outputLengthBytes) / ((double) inputLengthBytes) * 100;

      System.out.printf(" input: %d bytes [%d bits]%n", inputLengthBytes, inputLengthBits);
      System.out.printf("output: %d bytes [header: %d bits; encoding: %d bits]%n", outputLengthBytes, headerLength, encodedLength);
      System.out.printf("output/input size: %.4f%%%n", percentChange);
    } catch (Exception e) {
      System.err.printf("Error: %s%n", e.getMessage());
      System.exit(1);
    }
  }

  private Map<Byte, Integer> sortFreqsMap (Map<Byte, Integer> freqs) {
    Map<Byte, Integer> freqsMap = new LinkedHashMap<>();
    List<Map.Entry<Byte, Integer>> freqsList = new LinkedList<>(freqs.entrySet());

    Collections.sort(freqsList, new Comparator<Map.Entry<Byte, Integer>>() {
      public int compare (Map.Entry<Byte, Integer> a,
                          Map.Entry<Byte, Integer> b) {
        int value = a.getValue().compareTo(b.getValue());
        if (value == 0) {
          return a.getKey().compareTo(b.getKey());
        } else {
          return value;
        }            
      }
    });

    for (Map.Entry<Byte, Integer> entry : freqsList) {
      freqsMap.put(entry.getKey(), entry.getValue());
    }
    return freqsMap;
  }

  private Map<Byte, String> sortCodeMap (Map<Byte, String> codes) {
    Map<Byte, String> codeMap = new LinkedHashMap<>();
    List<Map.Entry<Byte, String>> encodedList = new LinkedList<>(codes.entrySet());

    Collections.sort(encodedList, new Comparator<Map.Entry<Byte, String>>() {
      public int compare (Map.Entry<Byte, String> a,
                          Map.Entry<Byte, String> b) {
        if (a.getValue().length() == (b.getValue().length())) {
          return a.getValue().compareTo(b.getValue());
        } else {
          return a.getValue().length() - (b.getValue().length());
        }
      }
    });

    for (Map.Entry<Byte, String> entry : encodedList) {
      codeMap.put(entry.getKey(), entry.getValue());
    }
    return codeMap;
  }

  private void decode (BitInputStream in, BitOutputStream out) {
    try {
      List<Map.Entry<Byte, String>> codeMapEntries = new ArrayList<>(codeMap.entrySet());
      int inputLength = in.readInt();

      recoverCodeTree(in, "", codeMap);

      String codeToBeMatched = "";

      Map<String, Byte> codeMapReversed = new HashMap<>();
      for (Map.Entry<Byte, String> entry : codeMap.entrySet()) {
        codeMapReversed.put(entry.getValue(), entry.getKey());
      }

      for (int i=0; i<inputLength;) {
        codeToBeMatched += in.readBit();
        if (codeMap.containsValue(codeToBeMatched)) {

          encodedString += codeToBeMatched;
          Byte letter = codeMapReversed.get(codeToBeMatched);

          if (freqsMap.containsKey(letter)) {
            freqsMap.put(letter, freqsMap.get(letter)+1);
          } else {
            freqsMap.put(letter, 1);
          }

          out.writeByte(letter);
          ++i;
          codeToBeMatched = "";
        }
      }

      codeMap = sortCodeMap(codeMap);

      if (settings.hasOption(SHOW_FREQUENCY)) {
        printFreqs(freqsMap);
      }
      if (settings.hasOption(SHOW_CODES)) {
        printCodes(codeMap);
      }
      if (settings.hasOption(SHOW_BINARY)) {
        printBinary(encodedString);
      }

      System.out.printf("original size: %d%n", inputLength);
    } catch (Exception e) {
      System.err.printf("Error: %s%n", e.getMessage());
      System.exit(1);
    }
  }

  private int recoverCodeTree (BitInputStream in, String code, Map<Byte, String> codeMap) throws IOException {
    int bit = in.readBit();
    if (bit == 0) {
      recoverCodeTree(in, code + "0", codeMap);
      recoverCodeTree(in, code + "1", codeMap);
    } else if (bit == 1) {
      byte letter = (byte) in.readByte();
      codeMap.put(letter, code);
    }
    return 0;
  }

  private Map<Byte, Integer> countFrequencies (byte[] data) {
    Map<Byte, Integer> frequencies = new HashMap<>();
    for (int i=0; i<data.length; ++i) {
      Integer count = frequencies.get(data[i]);
      if (count == null) {
        count = 0;
      }
      frequencies.put(data[i], count+1);
    }
    return frequencies;
  }

  private Node buildTree (Map<Byte, Integer> freq) {
    PriorityQueue<Node> forest = buildForest(freq);
    while (forest.size() > 1) {
      Node left = forest.remove();
      Node right = forest.remove();
      forest.add(new DecisionNode(left, right));
    }
    return forest.remove();
  }

  private PriorityQueue<Node> buildForest (Map<Byte, Integer> frequencies) {
    PriorityQueue<Node> forest;
    forest = new PriorityQueue<>(frequencies.size());
    for (Map.Entry<Byte, Integer> entry : frequencies.entrySet()) {
      forest.add(new ValueNode(entry.getKey(), entry.getValue()));
    }
    return forest;
  }

  abstract class Node implements Comparable<Node> {
    protected Integer count;

    public final Map<Byte, String> getAllCodes () {
      Map<Byte, String> codeTable = new HashMap<>();
      this.putCodes(codeTable, "");
      return codeTable;
    }

    abstract void putCodes (Map<Byte, String> table, String bits);

    public int compareTo (Node that) {
      return this.count.compareTo(that.count);
    }

    abstract void writeTo (BitOutputStream out) throws IOException;
  }

  private class DecisionNode extends Node {
    public Node left;
    public Node right;

    public DecisionNode (Node left, Node right) {
      this.left = left;
      this.right = right;
      this.count = left.count + right.count;
    }

    public void putCodes (Map<Byte, String> table, String bits) {
      left.putCodes(table, bits + "0");
      right.putCodes(table, bits + "1");
    }

    public void writeTo (BitOutputStream out) throws IOException {
      out.writeBit(0);
      left.writeTo(out);
      right.writeTo(out);
    }
  }

  private class ValueNode extends Node {
    public Byte value;

    public ValueNode (Byte value, Integer count) {
      this.value = value;
      this.count = count;
    }

    public void putCodes (Map<Byte, String> table, String bits) {
      table.put(value, bits);
    }

    public int compareTo (Node that) {
      return this.count.compareTo(that.count);
    }

    public void writeTo (BitOutputStream out) throws IOException {
      out.writeBit(1);
      out.writeByte(this.value);
    }
  }
}

import java.io.*;

public class BitInputStream implements AutoCloseable
{
  private byte[] bytes;
  private int index;

  public BitInputStream(File in) throws FileNotFoundException, IOException {
    this(new FileInputStream(in));
  }

  public BitInputStream(FileInputStream fileInputStream) throws IOException {
    DataInputStream in = new DataInputStream(fileInputStream);
    int size = in.available();
    this.bytes = new byte[size];
    this.index = 0;
    in.read(bytes);
    in.close();
  }

  public byte[] allBytes() {
    return bytes;
  }

  public int readBit() throws IOException {
    byte b = bytes[index / 8];
    int offset = index % 8;
    final int mask = 1 << 7;
    ++index;
    return (b & (mask >>> offset)) == 0 ? 0 : 1;
  }

  public int readByte() throws IOException {
    byte value = 0;
    for (int i = 0; i < 8; ++i) {
      value <<= 1;
      value |= readBit();
    }
    return value & 0xFF;
  }
  
  public int readInt() throws IOException {
    int value = readByte();
    value = (value << 8) | readByte();
    value = (value << 8) | readByte();
    value = (value << 8) | readByte();
    return value;
  }

  public void close() throws IOException {
    // intentionally left blank
  }
}

/*
 * MIT License
 *
 * Copyright (c) 2022-2024 H. Thevindu J. Wijesekera
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.clipshare.protocol;

import com.clipshare.netConnection.ServerConnection;
import com.clipshare.platformUtils.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class ProtoMethods {
  private static final int MAX_TEXT_LENGTH = 4194304; // 4 MiB
  private static final int MAX_FILE_NAME_LENGTH = 2048;
  private static final long MAX_FILE_SIZE = 17179869184L; // 16 GiB
  private static final long MAX_IMAGE_SIZE = 268435456; // 256 MiB
  private static final byte GET_TEXT = 1;
  private static final byte SEND_TEXT = 2;
  private static final byte GET_FILE = 3;
  private static final byte SEND_FILE = 4;
  private static final byte GET_IMAGE = 5;
  private static final byte GET_COPIED_IMAGE = 6;
  private static final byte GET_SCREENSHOT = 7;
  private static final byte INFO = 125;

  private static final byte STATUS_OK = 1;
  private static final int BUF_SZ = 65536;

  private final ServerConnection serverConnection;
  private final Utils utils;

  ProtoMethods(ServerConnection serverConnection, Utils utils) {
    this.serverConnection = serverConnection;
    this.utils = utils;
  }

  String v1_getText() {
    if (methodInit(GET_TEXT)) {
      return null;
    }
    return readString(MAX_TEXT_LENGTH);
  }

  boolean v1_sendText(String text) {
    if (text == null) {
      return false;
    }
    if (methodInit(SEND_TEXT)) {
      return false;
    }
    return !sendString(text);
  }

  boolean v1_getFiles() {
    return getFilesCommon(1);
  }

  boolean v1_sendFile() {
    if (!utils.prepareNextFile()) return false;
    String fileName = utils.getFileName();
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }
    long fileSize = utils.getFileSize();
    if (fileSize < 0) {
      return false;
    }
    InputStream inStream = utils.getFileInStream();
    if (inStream == null) {
      return false;
    }
    if (methodInit(SEND_FILE)) {
      return false;
    }
    if (sendString(fileName)) {
      return false;
    }
    if (sendSize(fileSize)) {
      return false;
    }
    byte[] buf = new byte[BUF_SZ];
    while (fileSize > 0) {
      int read_sz = (int) Math.min(fileSize, BUF_SZ);
      try {
        read_sz = inStream.read(buf, 0, read_sz);
      } catch (IOException ex) {
        return false;
      }
      if (read_sz < 0) {
        return true;
      } else if (read_sz == 0) {
        continue;
      }
      fileSize -= read_sz;
      if (this.serverConnection.send(buf, 0, read_sz)) {
        return false;
      }
    }
    return true;
  }

  private boolean getImageCommon(byte method, int display) {
    if (methodInit(method)) {
      return false;
    }
    if (method == GET_SCREENSHOT && selectDisplay(display)) return false;
    long file_size;
    try {
      file_size = readSize();
    } catch (IOException ignored) {
      return false;
    }
    if (file_size <= 0 || file_size > MAX_IMAGE_SIZE) {
      return false;
    }
    OutputStream out = utils.getFileOutStream(null);
    if (out == null) {
      return false;
    }
    byte[] buf = new byte[BUF_SZ];
    while (file_size > 0) {
      int read_sz = (int) Math.min(file_size, BUF_SZ);
      if (this.serverConnection.receive(buf, 0, read_sz)) {
        return false;
      }
      file_size -= read_sz;
      try {
        out.write(buf, 0, read_sz);
      } catch (IOException ex) {
        return false;
      }
    }
    try {
      out.close();
      utils.finish();
    } catch (IOException ignored) {
    }
    return true;
  }

  private boolean getImageCommon(byte method) {
    return getImageCommon(method, 0);
  }

  boolean v1_getImage() {
    return getImageCommon(GET_IMAGE);
  }

  String v1_checkInfo() {
    if (methodInit(INFO)) {
      return null;
    }
    try {
      String info = readString(MAX_FILE_NAME_LENGTH);
      if (info == null || info.isEmpty()) {
        return null;
      }
      return info;
    } catch (Exception ignored) {
      return null;
    }
  }

  private boolean getFilesCommon(int version) {
    if (methodInit(GET_FILE)) {
      return false;
    }
    long fileCnt;
    try {
      fileCnt = readSize();
    } catch (IOException ignored) {
      return false;
    }
    boolean status = true;
    for (long fileNum = 0; fileNum < fileCnt; fileNum++) {
      String fileName = readString(MAX_FILE_NAME_LENGTH);
      if (fileName == null || fileName.isEmpty()) {
        status = false;
        break;
      }
      if (version == 1 && fileName.contains("/")) {
        status = false;
        break;
      }
      long file_size;
      try {
        file_size = readSize();
      } catch (IOException ignored) {
        status = false;
        break;
      }
      if (file_size > MAX_FILE_SIZE) {
        status = false;
        break;
      }
      if (version == 3 && file_size < 0) {
        status &= utils.createDirectory(fileName);
        continue;
      } else if (file_size < 0) {
        status = false;
        break;
      }
      OutputStream out = utils.getFileOutStream(fileName);
      if (out == null) {
        status = false;
        break;
      }
      byte[] buf = new byte[BUF_SZ];
      while (file_size > 0) {
        int read_sz = (int) Math.min(file_size, BUF_SZ);
        if (this.serverConnection.receive(buf, 0, read_sz)) {
          status = false;
          break;
        }
        file_size -= read_sz;
        try {
          out.write(buf, 0, read_sz);
        } catch (IOException ex) {
          status = false;
          break;
        }
      }
      try {
        out.close();
      } catch (IOException ignored) {
      }
      if (!status) break;
    }
    return status && utils.finish();
  }

  private boolean selectDisplay(int display) {
    if (sendSize(display)) return true;
    byte[] status = new byte[1];
    return (this.serverConnection.receive(status) || status[0] != STATUS_OK);
  }

  /**
   * Reads a 64-bit signed integer from server
   *
   * @throws IOException on failure
   * @return integer received
   */
  private long readSize() throws IOException {
    byte[] data = new byte[8];
    if (this.serverConnection.receive(data)) {
      throw new IOException();
    }
    long size = 0;
    for (byte b : data) {
      size = (size << 8) | (b & 0xFF);
    }
    return size;
  }

  /**
   * Sends a 64-bit signed integer to server
   *
   * @param size value to be sent
   * @return false on success or true on error
   */
  private boolean sendSize(long size) {
    byte[] data = new byte[8];
    for (int i = data.length - 1; i >= 0; i--) {
      data[i] = (byte) (size & 0xFF);
      size >>= 8;
    }
    return this.serverConnection.send(data);
  }

  /**
   * Initializes the method
   *
   * @param method method code
   * @return false on success or true on failure
   */
  private boolean methodInit(byte method) {
    byte[] methodArr = {method};
    if (this.serverConnection.send(methodArr)) {
      return true;
    }
    byte[] status = new byte[1];
    return (this.serverConnection.receive(status) || status[0] != STATUS_OK);
  }

  /**
   * Reads a non-empty String, encoded with UTF-8, from server
   *
   * @param maxSize maximum size to read
   * @return read string or null on error
   */
  private String readString(int maxSize) {
    long size;
    try {
      size = this.readSize();
    } catch (IOException ignored) {
      return null;
    }
    if (size <= 0 || size > maxSize) {
      return null;
    }
    byte[] data = new byte[(int) size];
    if (this.serverConnection.receive(data)) {
      return null;
    }
    return new String(data, StandardCharsets.UTF_8);
  }

  /**
   * Sends a String encoded with UTF-8 to server
   *
   * @param data String to be sent
   * @return false on success or true on error
   */
  private boolean sendString(String data) {
    if (data == null) return true;
    final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
    final int len = bytes.length;
    if (len >= 16777216) return true;
    if (this.sendSize(len)) return true;
    return this.serverConnection.send(bytes);
  }

  /** Close the connection used for communicating with the server */
  public void close() {
    try {
      if (this.serverConnection != null) this.serverConnection.close();
    } catch (Exception ignored) {
    }
  }
}

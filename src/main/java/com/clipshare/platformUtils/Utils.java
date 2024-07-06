/*
 * MIT License
 *
 * Copyright (c) 2024 H. Thevindu J. Wijesekera
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

package com.clipshare.platformUtils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class Utils {

  private long fileSize;
  private String fileName;
  private InputStream inStream;
  private final long id;
  private static final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
  private LinkedList<File> pendingFiles;

  public Utils(File[] files) {
    this.pendingFiles = new LinkedList<>();
    this.pendingFiles.addAll(Arrays.asList(files));
    Random rnd = new Random();
    long idNum = Math.abs(rnd.nextLong());
    File file;
    do {
      String tmpDirName = Long.toString(idNum, 36);
      file = new File(tmpDirName);
      idNum++;
    } while (file.exists());
    this.id = idNum;
  }

  public Utils() {
    this.fileSize = -1;
    this.fileName = null;
    this.inStream = null;
    this.id = 0;
  }

  public static void setClipboardText(String text) {
    try {
      StringSelection testData = new StringSelection(text);
      clip.setContents(testData, testData);
    } catch (Exception ignored) {
    }
  }

  public static String getClipboardText() {
    String copiedStr = null;
    try {
      Transferable t = clip.getContents(null);
      if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        copiedStr = (String) t.getTransferData(DataFlavor.stringFlavor);
      }
    } catch (Exception ignored) {
    }
    return copiedStr;
  }

  private String getDataDirPath(String path) {
    File connDataDir = new File(String.valueOf(this.id));
    if (!connDataDir.exists()) {
      if (!connDataDir.mkdir()) {
        return null;
      }
    }
    return this.id + "/" + path;
  }

  public OutputStream getFileOutStream(String filePath) {
    try {
      if (filePath == null) {
        long tm = System.currentTimeMillis();
        filePath = Long.toString(tm, 36) + ".png";
      } else {
        int base_ind = filePath.lastIndexOf('/') + 1;
        String path = filePath.substring(0, base_ind);
        if (path.startsWith("../") || path.endsWith("/..") || path.contains("/../")) return null;
        filePath = getDataDirPath(filePath);
        if (filePath == null) return null;
      }
      File f = new File(filePath);
      String tmp_name;
      int pref = 1;
      while (f.exists()) {
        tmp_name = pref++ + "_" + filePath;
        f = new File(tmp_name);
      }
      return new FileOutputStream(f);
    } catch (Exception ignored) {
    }
    return null;
  }

  public boolean createDirectory(String dirPath) {
    dirPath = getDataDirPath(dirPath);
    if (dirPath == null) return false;
    File fp = new File(dirPath);
    if (fp.isDirectory()) return true;
    if (fp.exists()) return false;
    return fp.mkdirs();
  }

  public int getRemainingFileCount(boolean includeLeafDirs) {
    if (this.pendingFiles != null) return this.pendingFiles.size();
    return -1;
  }

  public int getRemainingFileCount() {
    return this.getRemainingFileCount(false);
  }

  public boolean finish() {
    File dataDir = new File(String.valueOf(this.id));
    if (!dataDir.exists()) {
      return true;
    }
    String[] content = dataDir.list();
    if (content == null) {
      return true;
    }
    boolean status = true;
    for (String fName : content) {
      File newFile = new File(fName);
      int pref = 1;
      while (newFile.exists()) {
        String newName = pref++ + "_" + fName;
        newFile = new File(newName);
      }
      File file = new File(this.id + "/" + fName);
      status &= file.renameTo(newFile);
    }
    status &= dataDir.delete();
    return status;
  }

  public boolean prepareNextFile() {
    try {
      if (this.pendingFiles == null || this.pendingFiles.isEmpty()) return false;
      File f = this.pendingFiles.pop();
      this.fileName = f.getName();
      this.fileSize = f.length();
      this.inStream = new FileInputStream(f);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  public String getFileName() {
    return this.fileName;
  }

  public long getFileSize() {
    return this.fileSize;
  }

  public InputStream getFileInStream() {
    return this.inStream;
  }
}

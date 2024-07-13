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

package com.clipshare;

import com.clipshare.netConnection.PlainConnection;
import com.clipshare.netConnection.ServerConnection;
import com.clipshare.platformUtils.Utils;
import com.clipshare.protocol.Proto;
import com.clipshare.protocol.Proto_v3;
import com.clipshare.protocol.ProtocolSelector;
import java.io.*;
import java.net.*;
import java.util.*;

public class Main {

  public static final short APP_PORT = 4337;

  public static void main(String[] args) {
    Scanner scan = new Scanner(System.in);
    String command;
    Inet4Address serverAddr = null;
    while (true) {
      try {
        System.out.print("Enter server address or enter 'sc' to scan : ");
        command = scan.nextLine();
        if ("-1".equals(command)) {
          scan.close();
          return;
        }
        if ("sc".equals(command)) {
          List<InetAddress> addresses = ServerFinder.find(APP_PORT, APP_PORT);
          if (addresses == null || addresses.isEmpty()) {
            System.out.println("Scan failed");
          } else {
            serverAddr = (Inet4Address) addresses.get(0);
            addresses.clear();
            if (serverAddr == null) {
              System.out.println("Scan failed");
            }
          }
        } else {
          serverAddr = (Inet4Address) Inet4Address.getByName(command);
        }
        if (serverAddr != null) {
          break;
        }
      } catch (Exception ignored) {
      }
    }
    do {
      System.out.print("Enter command ( or 'q' to stop): ");
      try {
        command = scan.nextLine();
      } catch (Exception ignored) {
        command = "-1";
        continue;
      }
      try {
        if (command.equals("h")) {
          System.out.println("Supported commands");
          System.out.println("h  : Display this help");
          System.out.println("g  : Get copied text");
          System.out.println("s  : Send copied text");
          System.out.println("i  : Get screenshot or copied image");
          System.out.println("ic : Get copied image if available");
          System.out.println(
              "is [display_number] : Get screenshot even if image is copied\n"
                  + "    display_number can be 1 or above. It can be ignored to use the default.");
          System.out.println("fg : Get copied files");
          System.out.println(
              "fs [file_1] [file_2] [file_3] : Send files.\n"
                  + "    File paths may be absolute or relative to current working directory.");
          System.out.println("q  : Quit");
        } else if (command.startsWith("fs ")) {
          String[] md = command.split("\\s+");
          File[] files = new File[md.length - 1];
          for (int i = 1; i < md.length; i++) {
            String fileName = md[i];
            files[i - 1] = new File(fileName);
          }
          Utils utils = new Utils(files);
          while (utils.getRemainingFileCount() > 0) {
            ServerConnection con = new PlainConnection(serverAddr, APP_PORT);
            Proto pr = ProtocolSelector.getProto(con, utils);
            if (pr.sendFile()) {
              System.out.println("Sending completed");
            } else {
              System.out.println("Error while sending file(s)!");
            }
            pr.close();
          }
        } else if (command.equals("fg")) {
          ServerConnection con = new PlainConnection(serverAddr, APP_PORT);
          Utils utils = new Utils();
          Proto pr = ProtocolSelector.getProto(con, utils);
          if (pr.getFile()) {
            System.out.println("Done");
          } else {
            System.out.println("Error while getting file(s)!");
          }
          pr.close();
        } else if (command.equals("g")) {
          ServerConnection con = new PlainConnection(serverAddr, APP_PORT);
          Proto pr = ProtocolSelector.getProto(con, null);
          String clip = pr.getText();
          if (clip != null) {
            Utils.setClipboardText(clip);
            System.out.println("Done");
          } else {
            System.out.println("Getting text failed!");
          }
          pr.close();
        } else if (command.equals("s")) {
          ServerConnection con = new PlainConnection(serverAddr, APP_PORT);
          String clip = Utils.getClipboardText();
          if (clip != null) {
            Proto pr = ProtocolSelector.getProto(con, null);
            if (pr.sendText(clip)) {
              System.out.println("Done");
            } else {
              System.out.println("Sending text failed!");
            }
            pr.close();
          } else {
            System.out.println("No text copied");
          }
        } else if (command.equals("i")) {
          ServerConnection con = new PlainConnection(serverAddr, APP_PORT);
          Utils utils = new Utils();
          Proto pr = ProtocolSelector.getProto(con, utils);
          if (pr.getImage()) {
            System.out.println("Done");
          } else {
            System.out.println("Error while getting image!");
          }
          pr.close();
        } else if (command.equals("is") || command.startsWith("is ")) {
          String[] parts = command.split("\\s+", 2);
          int display = 0;
          if (parts.length >= 2) {
            try {
              display = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
              System.out.println("Invalid display number");
            }
          }
          ServerConnection con = new PlainConnection(serverAddr, APP_PORT);
          Utils utils = new Utils();
          Proto pr = ProtocolSelector.getProto(con, utils);
          if (pr instanceof Proto_v3) {
            Proto_v3 pr3 = (Proto_v3) pr;
            if (pr3.getScreenshot(display)) {
              System.out.println("Done");
            } else {
              System.out.println("Error while getting image!");
            }
          } else {
            System.out.println("The server does not support this option");
          }
          pr.close();
        } else if (command.equals("ic")) {
          ServerConnection con = new PlainConnection(serverAddr, APP_PORT);
          Utils utils = new Utils();
          Proto pr = ProtocolSelector.getProto(con, utils);
          if (pr instanceof Proto_v3) {
            Proto_v3 pr3 = (Proto_v3) pr;
            if (pr3.getCopiedImage()) {
              System.out.println("Done");
            } else {
              System.out.println("Error while getting image!");
            }
          } else {
            System.out.println("The server does not support this option");
          }
          pr.close();
        }
      } catch (Exception ignored) {
        System.out.println("Error occurred! try again");
      }
    } while (!"q".equalsIgnoreCase(command));
    System.out.println("Bye!");
  }
}

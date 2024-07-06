![Last commit](https://img.shields.io/github/last-commit/thevindu-w/clip_share_desktop.svg?color=yellow)
![License](https://img.shields.io/github/license/thevindu-w/clip_share_desktop.svg?color=blue)

# ClipShare Desktop Client

### This is the desktop client of ClipShare.

Share the clipboard between your phone and desktop. Share files and screenshots securely.
<br>
ClipShare is a lightweight, cross-platform app for sharing copied text, files, and screenshots between an Android mobile
and a desktop.

## Download

- Server: Available on <a href="https://github.com/thevindu-w/clip_share_server/releases/latest">GitHub Releases</a>
- Client:
  - Desktop: Available on <a href="https://github.com/thevindu-w/clip_share_desktop/releases/latest">GitHub Releases</a> (this repository)
  - Mobile (Android): Available at,
    - <a href="https://apt.izzysoft.de/fdroid/index/apk/com.tw.clipshare">
      apt.izzysoft.de/fdroid/index/apk/com.tw.clipshare</a>
    - <a href="https://github.com/thevindu-w/clip_share_client/releases">GitHub Releases</a>
<br>

<p align="justify">
This is the desktop client of ClipShare. You will need the server running on another machine to connect with it.
ClipShare is lightweight and easy to use. Run the server on your Windows, macOS, or Linux machine to use the ClipShare
app. You can find more information on running the server on Windows, macOS, or Linux at
<a href="https://github.com/thevindu-w/clip_share_server#how-to-use">github.com/thevindu-w/clip_share_server</a>.
</p>

## Dependencies

- Java version 11 or above (JRE is sufficient)

You need to have the path to `java` set in the `PATH` environment variable if you run this from CLI.

## How to use

1. Start the client<br>
You can run the jar file from the Terminal / Command Prompt.
```bash
  java -jar ClipShare-1.0.0.jar
  ```
  You will now enter into the ClipShare client CLI.

2. Enter `sc` to scan for servers or enter the IPv4 address of the server.
3. Enter the commands to use ClipShare. You can find the commands and their usage from the help option by issuing the command `h` at the prompt.
4. Enter the command `q` to quit.
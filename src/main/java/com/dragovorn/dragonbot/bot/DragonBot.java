package com.dragovorn.dragonbot.bot;

import com.dragovorn.dragonbot.Utils;
import com.dragovorn.dragonbot.exceptions.ConnectionException;
import com.dragovorn.dragonbot.log.DragonLogger;
import com.dragovorn.dragonbot.log.LoggingOutputStream;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * *************************************************************************
 * (c) Dragovorn 2016. This file was created by Andrew at 3:27 PM.
 * as of 8/5/16 the project dragonbot is Copyrighted.
 * *************************************************************************
 */
public class DragonBot extends Bot {

    public volatile boolean isRunning;

    private String name; // Make it the default config name in start()
    private String charset;
    private String channelPrefixes = "#&+!";
    private List<String> mods = new ArrayList<>();

    private Connection connection;

    private InetAddress inetAddress;

    private InputThread inputThread;

    private OutputThread outputThread;

    private Queue outQueue = new Queue();

    private final Logger logger;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public DragonBot() throws IOException {
        logger = new DragonLogger("Dragon Bot", format.format(new Date()) + "-%g.log");
        System.setErr(new PrintStream(new LoggingOutputStream(logger, Level.SEVERE), true));
        System.setOut(new PrintStream(new LoggingOutputStream(logger, Level.INFO), true));
    }

    public static DragonBot getInstance() {
        return (DragonBot) Bot.getInstance();
    }

    @Override
    public void start() throws Exception {
        getLogger().info("Initializing Dragon Bot v" + getVersion() + "!");

        getLogger().info("Connecting to twitch!");
//        connect("irc.twitch.tv", 6667, "");
//        sendRawLine("CAP REQ :twitch.tv/membership");
//        sendRawLine("CAP REQ :twitch.tv/commands");
//        sendRawLine("CAP REQ :twitch.tv/tags");

        DragonBot.this.isRunning = true;

        getLogger().info("Dragon Bot v" + getVersion() + " initialized!");
    }

    @Override
    public void stop() {
        new Thread("Shutdown Thread") {

            @Override
            public void run() {
                DragonBot.this.isRunning = false;

                getLogger().info("Thank you and goodbye!");

                for (Handler handler : getLogger().getHandlers()) {
                    handler.close();
                }

                System.exit(0);
            }
        }.start();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVersion() {
        return Version.PRETTY_VERSION;
    }

    @Override
    public String getAuthor() {
        return "Dragovorn";
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public File getPluginsFolder() {
        return null;
    }

    @Override
    public String getChannel() {
        return this.connection.getChannel();
    }

    @Override
    public synchronized void connectTo(String channel) {
        connection.setChannel(channel);

        this.sendRawLine("JOIN " + channel);
    }

    @Override
    public void leaveChannel() {
        this.sendRawLine("PART " + connection.getChannel());

        connection.setChannel("");
    }

    @Override
    public synchronized void connect(String ip, int port, String password) throws ConnectionException, IOException {
        if (isConnected()) {
            throw new ConnectionException("You are already connected to a server!");
        }

        this.connection = new Connection(ip, port, password);

        Socket socket;

        if (connection.useSSL()) {
            try {
                SocketFactory factory;

                if (connection.verifySSL()) {
                    factory = SSLSocketFactory.getDefault();
                } else {
                    SSLContext context = UnverifiedSSL.getUnverifedSSLContext();
                    factory = context.getSocketFactory();
                }

                socket = factory.createSocket(connection.getServer(), connection.getPort());
            } catch (Exception exception) {
                throw new SSLException("SSL Failure");
            }
        } else {
            socket = new Socket(connection.getServer(), connection.getPort());
        }

        getLogger().info("Connected to server!");

        inetAddress = socket.getInetAddress();

        InputStreamReader reader;
        OutputStreamWriter writer;

        if (getEncoding() != null) {
            reader = new InputStreamReader(socket.getInputStream(), getEncoding());
            writer = new OutputStreamWriter(socket.getOutputStream(), getEncoding());
        } else {
            reader = new InputStreamReader(socket.getInputStream());
            writer = new OutputStreamWriter(socket.getOutputStream());
        }

        BufferedReader bufferedReader = new BufferedReader(reader);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        if (connection.getPassword() != null && !connection.getPassword().equals("")) {
            OutputThread.sendRawLine(this, bufferedWriter, "PASS " + connection.getPassword());
        }

        OutputThread.sendRawLine(this, bufferedWriter, "NICK " + this.getName());
        OutputThread.sendRawLine(this, bufferedWriter, "USER " + this.getName() + " 8 * :" + this.getName());

        inputThread = new InputThread(this, socket, bufferedReader, bufferedWriter);

        String line;

        while ((line = bufferedReader.readLine()) != null) {
            this.handleLine(line);

            int firstSpace = line.indexOf(" ");
            int secondSpace = line.indexOf(" ", firstSpace + 1);

            if (secondSpace >= 0) {
                String code = line.substring(firstSpace + 1, secondSpace);

                if (code.equals("004")) {
                    // We've connected
                    break;
                } else if (code.equals("433")) {
                    // No action required
                } else if (code.equals("439")) {
                    // No action required
                } else if (code.startsWith("5") || code.startsWith("4")) {
                    socket.close();

                    inputThread = null;

                    throw new ConnectionException("Could not log into the IRC Server: " + line);
                }
            }
        }

        getLogger().info("Logged into the twitch server!");

        socket.setSoTimeout(5 * 60 * 1000);

        inputThread.start();

        if (outputThread == null) {
            outputThread = new OutputThread(this, outQueue);
            outputThread.start();
        }

        // Fire BotConnectServerEvent
    }

    @Override
    public void sendMessage(String message) {
        if (connection.getChannel().equals("")) {
            return; // We are no in a channel then
        }

        outQueue.add("PRIVMSG " + connection.getChannel() + " :" + message);
    }

    @Override
    public void sendMessage(String message, Object... objects) {
        this.sendMessage(String.format(message, objects));
    }

    @Override
    public synchronized boolean isConnected() {
        return inputThread != null && inputThread.isConnected();
    }

    @Override
    protected void handleLine(String rawLine) {
        String line = CharMatcher.WHITESPACE.trimFrom(rawLine);

        if (line.startsWith("PING")) {
            sendRawLine("PONG " + line.substring(5));
            return;
        }

        ImmutableMap.Builder<String, String> tags = ImmutableMap.builder();

        if (line.startsWith("@")) {
            String v3Tags = line.substring(1, line.indexOf(" "));
            line = line.substring(line.indexOf(" ") + 1);

            StringTokenizer tokenizer = new StringTokenizer(v3Tags);

            while (tokenizer.hasMoreTokens()) {
                String tag = tokenizer.nextToken(";");

                if (tag.contains("=")) {
                    String[] parts = tag.split("=");
                    tags.put(parts[0], (parts.length == 2 ? parts[1] : ""));
                } else {
                    tags.put(tag, "");
                }
            }
        }

        List<String> parsedLine = Utils.tokenizeLine(line);

        String senderInfo = parsedLine.remove(0);
        String command = parsedLine.remove(0);
        String target = null;
        String nick = "";
        String hostname = "";
        String login = "";

        int exclamation = senderInfo.indexOf("!");
        int at = senderInfo.indexOf("@");

        if (senderInfo.startsWith(":")) {
            if (exclamation > 0 && at > 0 && exclamation < at) {
                nick = senderInfo.substring(1, exclamation);
                login = senderInfo.substring(exclamation + 1, at);
                hostname = senderInfo.substring(at + 1);
            } else {
                if (!parsedLine.isEmpty()) {
                    String token = command;

                    int code = -1;

                    try {
                        code = Integer.parseInt(token);
                    } catch (NumberFormatException exception) {
                        // Leave it at -1
                    }

                    if (code == -1) {
                        return;
                    } else {
                        nick = senderInfo;
                        target = token;
                    }
                }
            }
        }

        command = command.toUpperCase();

        if (nick.startsWith(":")) {
            nick = nick.substring(1);
        }

        if (target == null) {
            target = parsedLine.remove(0);
        }

        if (target.startsWith(":")) {
            target = target.substring(1);
        }

        if (command.equals("PRIVMSG") && line.indexOf(":\u0001") > 0 && line.endsWith("\u0001")) {
            String request = line.substring(line.indexOf(":\u0001") + 2, line.length() - 1);

            // TODO: Check for CTP Requests
        } else if (command.equals("PRIVMSG") && channelPrefixes.indexOf(target.charAt(0)) >= 0) {
            User user = new User(nick, login, hostname, tags.build());

            // Execute message event
            getLogger().info("CHAT " + (user.isMod() ? "" : "[M] ") + user.getName() + ": " + line.substring(line.indexOf(" :") + 2));
        }
    }

    @Override
    public synchronized void sendRawLine(String line) {
        if (isConnected()) {
            inputThread.sendRawLine(line);
        }
    }

    @Override
    public void sendRawLineViaQueue(String line) {
        if (line == null) {
            throw new NullPointerException("Cannot send null message to server");
        }

        if (isConnected()) {
            outQueue.add(line);
        }
    }

    @Override
    public int getMaxLineLength() {
        return InputThread.MAX_LINE_LENGTH;
    }

    @Override
    public int getMessageDelay() {
        return 1000;
    }

    @Override
    public void setEncoding(String charset) throws UnsupportedEncodingException {
        "".getBytes(charset); // Check if the charset is supported

        this.charset = charset;
    }

    @Override
    public String getEncoding() {
        return charset;
    }

    @Override
    public InetAddress getAddress() {
        return inetAddress;
    }
}
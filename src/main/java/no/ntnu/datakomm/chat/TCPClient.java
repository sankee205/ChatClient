package no.ntnu.datakomm.chat;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket socket;

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        // TODO Step 1: implement this method
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        boolean suc = false;
        System.out.println("connecting to client...");
        // InetSocketAddress serverAddress = new InetSocketAddress(host, port);
        try {
            this.socket = new Socket(host, port);
            this.toServer = new PrintWriter(this.socket.getOutputStream(), true);
            this.fromServer = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            System.out.println("Successfully connected");
            suc = true;

        } catch (IOException e) {
            System.out.println("Socket error: " + e.getMessage());
        }
        return suc;
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        // TODO Step 4: implement this method
        // Hint: remember to check if connection is active
        if (isConnectionActive()) {
            try {
                System.out.println("socket successfully closed");
                socket.close();
                socket = null;
                onDisconnect();
            } catch (IOException e) {
                System.out.println("Closing socket error" + e.getMessage());
            }
        }
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return socket != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // TODO Step 2: Implement this method
        // Hint: Remember to check if connection is active
        boolean statement = false;
        if (isConnectionActive()) {
            this.toServer.println(cmd);
            statement = true;
        }
        return statement;
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // TODO Step 2: implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        String msg = "msg" + " " + message;
        return this.sendCommand(msg);
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // TODO Step 3: implement this method
        // Hint: Reuse sendCommand() method
        this.sendCommand("login" + " " + username);

    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // TODO Step 5: implement this method
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
        if (isConnectionActive()) {
            this.sendCommand("users");
        }
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        // TODO Step 6: Implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        String privmsg = "privmsg" + " " + recipient+ " " + message;
        return this.sendCommand(privmsg);
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // TODO Step 8: Implement this method
        // Hint: Reuse sendCommand() method
        if (isConnectionActive()) {
            sendCommand("help");
        }
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        // TODO Step 3: Implement this method
        // TODO Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.

        String oneResponseLine = null;
        try {

            oneResponseLine = this.fromServer.readLine();
            if (oneResponseLine == null) {
                disconnect();

            }

        } catch (IOException e) {
            System.out.println("responseline error: " + e.getMessage());
        }

        return oneResponseLine;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        while (isConnectionActive()) {
            // TODO Step 3: Implement this method
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method

            // TODO Step 5: update this method, handle user-list response from the server
            // Hint: In Step 5 reuse onUserList() method

            // TODO Step 7: add support for incoming chat messages from other users (types: msg, privmsg)
            // TODO Step 7: add support for incoming message errors (type: msgerr)
            // TODO Step 7: add support for incoming command errors (type: cmderr)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners

            // TODO Step 8: add support for incoming supported command list (type: supported)
            String r = " ";
            while (isConnectionActive() && r != null) {
                // TODO Step 3: Implement this method
                r = waitServerResponse();
                if (r != null) {
                    String[] response = r.split(" ", 2);

                    String tmp;

                    switch (response[0]) {
                        case "users":
                            onUsersList(response[1].split(" "));
                            break;
                        case "msg":
                            String[] splitmsg = response[1].split(" ", 2);

                            onMsgReceived(false, splitmsg[0], splitmsg[1]);
                            break;
                        case "privmsg":
                            String[] splitPrivMsg = response[1].split(" ", 2);
                            onMsgReceived(true, splitPrivMsg[0], splitPrivMsg[1]);
                            break;

                        case "loginok":
                            onLoginResult(true, null);
                            break;
                        case "loginerr":
                            onLoginResult(false, response[0]);
                            break;
                        case "msgerr":
                            onMsgError(response[0]);
                            break;
                        case "cmderr":
                            onCmdError(response[0]);
                            break;
                        case "supported":
                            onSupported(response[0].split(" "));
                            break;


                    }
                }

            }
        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        // TODO Step 4: Implement this method
        // Hint: all the onXXX() methods will be similar to onLoginResult()
        for (ChatListener l : listeners) {
            l.onDisconnect();
        }
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        // TODO Step 5: Implement this method
        for (ChatListener l : listeners) {
            l.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        // TODO Step 7: Implement this method
        TextMessage message = new TextMessage(sender, priv, text);

        for (ChatListener l : listeners) {
            l.onMessageReceived(message);
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        // TODO Step 7: Implement this method
        errMsg = lastError;
        for (ChatListener l : listeners) {
            l.onMessageError(errMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // TODO Step 7: Implement this method
        for (ChatListener l : listeners) {
            l.onCommandError(errMsg);
        }
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        // TODO Step 8: Implement this method
        for (ChatListener l : listeners) {
            l.onSupportedCommands(commands);
        }
        System.out.println(commands);
    }
}

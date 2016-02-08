/**
 * Created by Matthew on 2/7/2016.
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

public class EmailServer {

    private static Connection conn = null;

    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/email";

    //  Database credentials
    static final String USER = "root";
    static final String PASS = "matthew";


    public static void main(String[] args) throws Exception {
        System.out.println("The Email server is now running.");
        int clientNumber = 0;
        ServerSocket listener = new ServerSocket(6789);



        Statement stmt = null;
        try{
            //STEP 2: Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            //STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
            try {
                while (true) {
                    new Email(listener.accept(), clientNumber++).start();
                }
            }
            catch (IOException e) {
                System.out.println("Error");
            }
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }
        //end try


        listener.close();

    }


    private static class Email extends Thread {
        private Socket socket;
        private int clientNumber;
        public String[] messages = new String[100];
        public int numberOfMessages = 0;
        public int id = 0;
        public boolean authenticated = false;

        public Email(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            System.out.println("New connection with client #" + clientNumber + ".");
        }


        public void run() {
            try {

                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());


                while (true) {
                    String fromClient = inFromClient.readLine();

                    if (fromClient.equals("createAccount")) {
                        String username = inFromClient.readLine();
                        String pass = inFromClient.readLine();
                        System.out.println("Recieved new account details from client: " + clientNumber + ".");
                        createAccount(username, pass);
                        authenticated = true;
                        id = getId(username);
                    }

                    if (fromClient.equals("login")) {
                        String username = inFromClient.readLine();
                        String pass = inFromClient.readLine();
                        String realPass = getPassword(username);
                        if (pass.equals(realPass)) {
                            outToClient.writeBytes("success\n");
                            authenticated = true;
                            id = getId(username);
                        }
                        else {
                            outToClient.writeBytes("failure\n");
                        }
                    }

                    if (fromClient.equals("sendMessage")) {
                        String newRecipient = inFromClient.readLine();
                        String newSubject = inFromClient.readLine();
                        String newMessage = inFromClient.readLine();
                        System.out.println("Recieved message from client: " + clientNumber + ".");

                        int recipient = getId(newRecipient);
                        sendMessage(recipient, newMessage, newSubject, id);
                    }

                    if (fromClient.equals("recieveSubjects")) {
                        String[] userSubjects = getSubjects();
                        outToClient.writeBytes(numberOfMessages * 2 + "\n");
                        for (int i = 0; i <numberOfMessages * 2; i++) {
                            outToClient.writeBytes(userSubjects[i] + '\n');
                        }
                        System.out.println("Sent all messages to client: " + clientNumber + ".");
                    }


                    if (fromClient.equals("recieveMessage")) {
                        String requestedSubject = inFromClient.readLine();
                        String requestedMessage = getMessage(requestedSubject);
                        outToClient.writeBytes(requestedMessage + "\n");
                        System.out.println("Sent requested message to client: " + clientNumber + ".");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error! Client #" + clientNumber + ".");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Couldn't close socket.");
                }
                System.out.println("Client #" + clientNumber + " has disconnected.");
            }
        }

        public String getMessage(String subject) {
            String requestedMessage = "";
            try{
                Statement stmt = null;
                stmt = conn.createStatement();
                String sql = "SELECT message FROM messages WHERE messageSubject = '" + subject + "' AND id=" + id;
                ResultSet myResultSet = stmt.executeQuery(sql);
                myResultSet.next();
                requestedMessage = myResultSet.getString("message");

                sql = "DELETE FROM messages WHERE message='" + requestedMessage + "' AND messageSubject='" + subject + "' AND id=" + id;
                System.out.println(sql);
                PreparedStatement preparedStmt = conn.prepareStatement(sql);
                preparedStmt.executeUpdate();
            }
            catch (java.sql.SQLException e) {
                System.out.println("SQL ERROR" + e);
            }
            return requestedMessage;
        }

        public String[] getSubjects() {
            String[] userSubjects = new String[100];
            int i = 0;
            try{
                Statement stmt = null;
                stmt = conn.createStatement();
                ResultSet myResultSet = stmt.executeQuery("SELECT MESSAGESUBJECT,SENDER FROM MESSAGES WHERE id=" + id);
                while (myResultSet.next()) {
                    userSubjects[i++] = myResultSet.getString("messageSubject");
                    int sender = myResultSet.getInt("sender");
                    userSubjects[i++] = getUsername(sender);
                }
            }
            catch (java.sql.SQLException e) {
                System.out.println("SQL ERROR" + e);
            }
            numberOfMessages = i / 2;
            return userSubjects;
        }

        public void sendMessage(int recipient, String message, String subject, int sender) {
            try{
                String sql = "INSERT INTO MESSAGES (id, messageSubject, message, sender) VALUES(?, ?, ?, ?)";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setInt(1, recipient);
                preparedStatement.setString(2, subject);
                preparedStatement.setString(3, message);
                preparedStatement.setInt(4, sender);
                preparedStatement.executeUpdate();
                numberOfMessages++;
            }
            catch (java.sql.SQLException e) {
                System.out.println("SQL ERROR" + e);
            }
        }

        public String getPassword(String username) {
            String password = "";
            try{
                Statement stmt = null;
                stmt = conn.createStatement();
                ResultSet myResultSet = stmt.executeQuery("SELECT pass FROM users WHERE username='" + username + "'");
                myResultSet.next();
                password = myResultSet.getString("pass");
            }
            catch (java.sql.SQLException e) {
                System.out.println("SQL ERROR" + e);
            }
            return password;
        }

        public void createAccount(String username, String pass) {
            try{
                String sql = "INSERT INTO users (username, pass) VALUES(?, ?)";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, pass);
                System.out.println(sql);
                preparedStatement.executeUpdate();
            }
            catch (java.sql.SQLException e) {
                System.out.println("SQL ERROR" + e);
            }
        }

        public int getId(String username) {
            int id = 0;
            try{
                Statement stmt = null;
                stmt = conn.createStatement();
                ResultSet myResultSet = stmt.executeQuery("SELECT id FROM users WHERE username='" + username + "'");
                myResultSet.next();
                id = myResultSet.getInt("id");
            }
            catch (java.sql.SQLException e) {
                System.out.println("SQL ERROR" + e);
            }
            return id;
        }

        public String getUsername(int id) {
            String username = "";
            try{
                Statement stmt = null;
                stmt = conn.createStatement();
                ResultSet myResultSet = stmt.executeQuery("SELECT username FROM users WHERE id='" + id + "'");
                myResultSet.next();
                username = myResultSet.getString("username");
            }
            catch (java.sql.SQLException e) {
                System.out.println("SQL ERROR" + e);
            }
            return username;
        }
    }
}

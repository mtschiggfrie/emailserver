/**
 * Created by Matthew on 2/7/2016.
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class EmailClient {
    public static void main(String argv[]) throws Exception
    {
        String userInput;
        boolean cont = true;
        Scanner scanner = new Scanner(System.in);
        Socket clientSocket = new Socket("", 6789);
        boolean authenticated = false;

        while(cont) {
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Press 1 to log in.");
            System.out.println("Press 2 to create an account.");

            userInput = scanner.nextLine();

            if (userInput.equals("1")) {
                System.out.println("Enter your username.");
                String username = scanner.nextLine();
                System.out.println("Enter your password.");
                String pass = scanner.nextLine();

                outToServer.writeBytes("login\n");
                outToServer.writeBytes(username + "\n");
                outToServer.writeBytes(pass + "\n");

                if (inFromServer.readLine().equals("success")) {
                    System.out.println("Login successful!");
                    authenticated = true;
                }
            }

            if (userInput.equals("2")) {
                System.out.println("Enter your desired username.");
                String username = scanner.nextLine();
                System.out.println("Enter your desired password.");
                String pass = scanner.nextLine();

                outToServer.writeBytes("createAccount\n");
                outToServer.writeBytes(username + "\n");
                outToServer.writeBytes(pass + "\n");

                System.out.println("Account creation successful!");
                authenticated = true;
            }

            System.out.println();

            while (authenticated) {
                //after you log in successfully
                System.out.println("Press 1 to send a message.");
                System.out.println("Press 2 to list subject from all messages.");
                System.out.println("Press 3 to quit.");

                userInput = scanner.nextLine();

                System.out.println();

                if (userInput.equals("1")) {
                    outToServer.writeBytes("sendMessage\n");
                    BufferedReader message = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Enter the username of the recipient of the message.");
                    outToServer.writeBytes(message.readLine() + '\n');
                    System.out.println();
                    System.out.println("Enter the subject of the message.");
                    outToServer.writeBytes(message.readLine() + '\n');
                    System.out.println();
                    System.out.println("Enter the message to send.");
                    outToServer.writeBytes(message.readLine() + '\n');
                    System.out.println("Message Sent!");
                } else if (userInput.equals("2")) {
                    outToServer.writeBytes("recieveSubjects\n");

                    System.out.println();

                    int numberOfSubjects = Integer.parseInt(inFromServer.readLine());
                    for (int i = 0; i < numberOfSubjects; i = i +2) {
                        System.out.print("Subject: ");
                        System.out.print(inFromServer.readLine());
                        System.out.print("      Sender: ");
                        System.out.println(inFromServer.readLine());
                    }

                    System.out.println();
                    System.out.println("Enter the subject whose email you would like to recieve");
                    System.out.println("Or press 'q' to go back to menu.");
                    String option = scanner.nextLine();
                    System.out.println();

                    if (!option.equals("q")) {
                        outToServer.writeBytes("recieveMessage\n");
                        outToServer.writeBytes(option + "\n");
                        String messageFromServer = inFromServer.readLine();
                        System.out.println("Message: " + messageFromServer);
                    }

                } else if (userInput.equals("3")) {
                    authenticated = false;
                    cont = false;
                }

                System.out.println();
                System.out.println();

            }
        }

        scanner.close();
        clientSocket.close();

    }
}


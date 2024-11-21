package com.br.featherchat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class FeatherClient implements Runnable {
  private Socket client;
  private BufferedReader in;
  private PrintWriter out;
  private boolean done = false;

  @Override
  public void run() {
    try {
      this.client = new Socket("localhost", 4444);
      out = new PrintWriter(this.client.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(this.client.getInputStream()));

      InputHandler inHandler = new InputHandler();
      Thread t = new Thread(inHandler);

      t.start();

      String inMessage;
      while ((inMessage = in.readLine()) != null) {
        System.out.println(inMessage);
      }

    } catch (Exception i) {
      System.out.println("An error occurred during the proccess: " + i.toString());
    }
  }

  class InputHandler implements Runnable {
    @Override
    public void run() {
      try {
        BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
        while (!done) {
          String message = inReader.readLine();
          // TODO: add some commands functions like /list_users_in_chat, /ban,
          // /update_nick and so on
          if (message.equals("/quit")) {
            inReader.close();
            shutdown();
          } else {
            out.println(message);
          }
        }
      } catch (Exception e) {
        System.out.println("An erro ocurred during the proccess: " + e.toString());
        shutdown();
      }
    }
  }

  public void shutdown() {
    System.out.println("Shuting down...");

    this.done = true;
    try {
      if (this.in != null) {
        this.in.close();
      }

      if (this.out != null) {
        this.out.close();
      }

      if (this.client != null && this.client.isClosed()) {
        this.client.close();
      }

    } catch (IOException e) {
      System.out.println("An error happened" + e.toString());
    }
  }

  public static void main(String[] args) {
    FeatherClient client = new FeatherClient();
    Thread clientThread = new Thread(client);
    clientThread.run();
  }
}

package com.br.featherchat.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class FeatherServer {

  public static void main(String[] args) {
    SpringApplication.run(FeatherServer.class, args);
  }
}

@Service
class ChatServer {

  @Value("${server.port:4444}")
  private int port;

  private ServerSocket serverSocket;
  private ExecutorService pool;
  private ArrayList<ConnHandler> connections;
  private boolean running;

  public ChatServer() {
    this.connections = new ArrayList<>();
    this.pool = Executors.newCachedThreadPool();
    this.running = false;
  }

  @PostConstruct
  public void startServer() {
    try {
      serverSocket = new ServerSocket(port);
      running = true;
      System.out.println("Servidor de chat iniciado na porta " + port);

      // Inicia o servidor em uma thread separada
      pool.execute(this::acceptConnections);
    } catch (IOException e) {
      e.printStackTrace();
      shutdown();
    }
  }

  private void acceptConnections() {
    try {
      while (running) {
        Socket clientSocket = serverSocket.accept();
        ConnHandler handler = new ConnHandler(clientSocket);
        synchronized (connections) {
          connections.add(handler);
        }
        pool.execute(handler);
      }
    } catch (IOException e) {
      e.printStackTrace();
      shutdown();
    }
  }

  @PreDestroy
  public void shutdown() {
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      pool.shutdown();
      System.out.println("Servidor de chat encerrado.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public class ConnHandler implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;

    public ConnHandler(Socket client) {
      this.client = client;
    }

    @Override
    public void run() {
      try {
        out = new PrintWriter(client.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        broadcast("Alguém entrou no chat!");

        String message;
        while ((message = in.readLine()) != null) {
          if (message.startsWith("/quit")) {
            broadcast("Alguém saiu.");
            shutdown();
            break;
          } else {
            broadcast(message);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        shutdown();
      }
    }

    public synchronized void broadcast(String message) {
      synchronized (connections) {
        for (ConnHandler ch : connections) {
          if (ch != null) {
            ch.sendMessage(message);
          }
        }
      }
    }

    public void sendMessage(String message) {
      try {
        out.println(message);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void shutdown() {
      try {
        in.close();
        out.close();
        if (!client.isClosed()) {
          client.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      synchronized (connections) {
        connections.remove(this);
      }
    }
  }
}

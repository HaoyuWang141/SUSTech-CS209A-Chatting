package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.client.Controller.ChatController;
import cn.edu.sustech.cs209.chatting.client.Controller.LoginController;
import cn.edu.sustech.cs209.chatting.client.Controller.SignUpController;
import cn.edu.sustech.cs209.chatting.common.ChatContent;
import cn.edu.sustech.cs209.chatting.common.LocalChat;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Request;
import cn.edu.sustech.cs209.chatting.common.RequestType;
import cn.edu.sustech.cs209.chatting.common.UploadedFile;
import cn.edu.sustech.cs209.chatting.common.User;
import com.vdurmont.emoji.EmojiParser;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Client {

  private static Client client;
  private final String host;
  private final int port;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private Scanner scanner;
  private String name;
  private Socket socket;
  public ChatController chatController;

  private Client(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public static void Connect(String host, int port) throws IOException {
    client = new Client(host, port);
    client.start();
  }

  public static Client getClient() {
    return client;
  }

  private void start() throws IOException {
    socket = new Socket(host, port);
    System.out.println("Connect to Server: " + socket.getRemoteSocketAddress());
    out = new ObjectOutputStream(socket.getOutputStream());
    in = new ObjectInputStream(socket.getInputStream());

    Thread receiveThread = new Thread(() -> {
      while (true) {
        try {
          Request<?> request = (Request) in.readObject();
          // System.out.println("Message from Server: " + request.getInfo());
          switch (request.getType()) {
            case Signup:
              SignUpController.getSignUpController().getOos()
                  .writeObject(request);
              break;
            case Login:
              LoginController.getLoginController().getOos()
                  .writeObject(request);
              break;
            case SendMessage:
            case SendFile:
              // System.out.println("CLient收到了返回值SendMessage");
              break;
            case UserList:
              chatController.setUserList((List<User>) request.getObj());
              break;
            case ChatGroupList:
              chatController.setChatList(
                  (List<LocalChat>) request.getObj());
              break;
            case ChatContent:
              chatController.setChatContent((ChatContent) request.getObj());
              break;
            case CreateChatGroup:
              chatController.setCurrentChatId((Integer) request.getObj());
              break;
            default:
          }
        } catch (NullPointerException e) {
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
          System.exit(1);
        } catch (SocketException e) {
          System.out.println(e.getMessage());
          in = null;
          out = null;
          socket = null;
          if (chatController != null) {
            chatController.serverDown();
          } else {
            System.exit(1);
          }
          break;
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(1);
        }
      }
    });
    receiveThread.start();
  }

  public <T> void sendRequest(RequestType type, String info, T obj) throws IOException {
    Request<T> request = new Request<>(type, true, info, obj);
    out.writeObject(request);
    out.flush();
  }

  public void sendMessage(String sendBy, int sendTo, String data) throws IOException {
    String sendData = EmojiParser.parseToAliases(data);
    Message message = new Message(sendBy, sendTo, sendData);
    out.writeObject(message);
    out.flush();
  }

  public void sendFile(String sendBy, int sendTo, File file) throws IOException {
    if (sendBy == null || file == null) {
      return;
    }
    String data = Files.readString(Paths.get(file.getAbsolutePath()));
    UploadedFile uploadedFile = new UploadedFile(file.getName(), data, sendBy, sendTo);
    out.writeObject(uploadedFile);
    out.flush();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void close() {
    try {
      if (socket != null && !socket.isClosed()) {
        sendRequest(RequestType.Disconnect, null, null);
        socket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.exit(0);
  }

  public void setChatController(ChatController chatController) {
    this.chatController = chatController;
  }
}

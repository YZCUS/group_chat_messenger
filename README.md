# Project: group_chat_messenger
This real-time chat application implements group chat feature leveraging the power of multithreading and socket programming in Java for efficient and responsive communication. Three-way handshakes mechanism between the server and clients upon connection ensures a robust and reliable connection process. On top of this, all messages are encrypted from any unauthorized access, enhancing security and user privacy.

## Description:



## Getting Started
### Enviromnent:
- JAVA: JDK 8

### Installing
- Clone this repository and navigate to directory
```bash
git clone https://github.com/YZCUS/group_chat_messenger.git
cd group_chat_messenger
```

- Compile Java files
```bash
javac -cp . src/encryption/Encryption.java
javac -cp src src/chat/ChatServer.java
javac -cp src src/chat/ChatClient.java
```

### Executing program
- Run Server
```bash
cd group_chat_messenger
java -cp src chat.ChatServer
```

- Run Client (You may open multiple clients simultaneously)
```bash
cd group_chat_messenger
java -cp src chat.ChatClient
```


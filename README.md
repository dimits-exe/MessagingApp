
# Yammi Messaging App

## Description

A Distributed System following the Publisher - Subscriber pattern. The project also includes an Application that acts as a client, sending and receiving messages, and a local file system for persistence.

![Client application](https://cdn.discordapp.com/attachments/354913879471423492/1021044231025983578/unknown.png)

The System is initialized with N servers (possibly in different machines) which can be connected to K clients. The clients can then send text, images and videos to various Topics (conversations). The servers automatically distribute the process load among themselves.


## Project Setup

### Building the project

Execute `build.bat` or compile every `.java` file in the `src` directory

#### Run the main Server:
Execute `run_server.bat` with no arguments or run `java --class-path bin app.Server`.

#### Run a secondary Server:
Secondary servers connect to the main server and are dynamically allocated Topics which they are responsible for managing.

Run `run_server.bat <ip> <broker_port>` or run `java --class-path bin app.Server <ip> <broker_port>`, where:
- `ip` and `broker_port` of the main server. These will be printed to the main server's console when it's initialized.

####  Run an Android Client:
Open the project in Android Studio and execute it. There's no limit to how many clients 

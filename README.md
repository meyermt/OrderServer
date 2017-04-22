# OrderProject
Simple REST-like API server for fictitious orders.

## Running this Project

You must run this project on a system that has Java 8 installed.

To more easily manage the two third-party libraries used in this code, this project uses Gradle. Since the Gradle wrapper
is used, there is no additional Gradle knowledge or setup required on devices building this project.

## How to Build and Run the Order Server

1. From the root directory, run `./gradlew installDist -PmainArg=OrderServer`
2. From the same (root) directory, run `build/install/OrderServer/bin/OrderProject --port <port>`
3. Your OrderServer should be up and running!

## How to Run the Order Client

1. From the root directory, run `./gradlew installDist -PmainArg=OrderClient`
2. From the same (root) directory, run `build/install/OrderClient/bin/OrderProject --ip <server ip> --serverPort <server port>`
3. Your OrderClient should be up and running!


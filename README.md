# GraviTune (Volume Control)

**GraviTune** is an advanced Android application designed to provide seamless and accessible volume control through a floating overlay interface. This project aims to enhance the user experience by allowing quick adjustments to multimedia and call volumes without interrupting current activities.

## Features

-   **Floating Overlay Control**: Access volume settings from anywhere on your screen.
-   **Dual Volume Management**: Independently control Multimedia and Call volumes.
-   **Foreground Service**: Ensures the overlay remains active and responsive.
-   **User-Centric Design**: Intuitive interface optimized for ease of use.

## Technical Stack

-   **Language**: Kotlin
-   **Platform**: Android
-   **Architecture**: MVVM (implied standard)
-   **Key Components**:
    -   `ForegroundService` for persistent overlay execution.
    -   `WindowManager` for floating UI rendering.

## Setup & Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/YOUR_USERNAME/GraviTune.git
    ```
2.  Open the project in **Android Studio**.
3.  Sync Gradle dependencies.
4.  Build and Run the application on an emulator or physical device.

## Permissions

The application requires the following permissions to function correctly:
-   `FOREGROUND_SERVICE`: To keep the app running.
-   `SYSTEM_ALERT_WINDOW`: To display the floating volume controls over other apps.
-   `POST_NOTIFICATIONS`: For service status updates (Android 13+).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

Developed by **Jhosue Acosta** as part of an advanced mobile engineering initiative.

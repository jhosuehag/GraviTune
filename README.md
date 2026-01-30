# GraviTune (Control de Volumen)

**GraviTune** es una aplicación avanzada para Android diseñada para brindarte un control total y accesible sobre el volumen de tu dispositivo a través de una interfaz flotante inteligente.

El objetivo principal es permitirte ajustar el volumen multimedia y de llamadas de forma rápida y cómoda, sin necesidad de interrumpir lo que estás haciendo, ya sea jugando, viendo videos o navegando por otras aplicaciones.

## 🚀 Características Principales

*   **📱 Control Flotante Siempre Visible**: Un botón discreto y elegante que puedes mover a cualquier parte de la pantalla para tener el control siempre a mano.
*   **🎚️ Gestión de Volumen Dual e Independiente**: Sube o baja el volumen de **Multimedia** (música, videos) y de **Llamadas** por separado, sin confusiones.
*   **⚡ Servicio en Segundo Plano Robusto**: La aplicación utiliza un servicio en primer plano (`Foreground Service`) para asegurar que el control no se cierre inesperadamente por el sistema.
*   **🎨 Diseño Centrado en el Usuario**: Interfaz intuitiva y minimalista que se integra perfectamente con la experiencia de Android.

## 🛠️ Tecnologías Utilizadas

Este proyecto ha sido construido siguiendo estándares modernos de desarrollo Android:

*   **Lenguaje**: [Kotlin](https://kotlinlang.org/) (100%)
*   **Arquitectura**: MVVM (Model-View-ViewModel) para un código limpio y mantenible.
*   **Componentes Clave**:
    *   `ForegroundService`: Garantiza la persistencia de la funcionalidad.
    *   `WindowManager`: Permite dibujar la interfaz de usuario sobre otras aplicaciones.
    *   `ViewBinding`: Para una interacción segura y eficiente con las vistas.

## 📦 Instalación y Configuración

Si deseas probar o contribuir al proyecto, sigue estos pasos:

1.  **Clonar el repositorio**:
    ```bash
    git clone https://github.com/jhosuehag/GraviTune.git
    ```
2.  Abrir el proyecto en **Android Studio**.
3.  Sincronizar las dependencias de Gradle.
4.  Compilar y ejecutar en tu dispositivo o emulador.

## 🔐 Permisos Requeridos

Para ofrecer su funcionalidad, GraviTune necesita los siguientes permisos. **Valoramos tu privacidad y estos permisos son estrictamente para el funcionamiento de la app**:

*   **Mostrar sobre otras apps (`SYSTEM_ALERT_WINDOW`)**: Indispensable para mostrar el botón flotante de volumen.
*   **Servicio en primer plano (`FOREGROUND_SERVICE`)**: Necesario para que la app se mantenga activa y no sea cerrada por el ahorro de batería del sistema.
*   **Notificaciones (`POST_NOTIFICATIONS`)**: Para mostrar el estado del servicio en la barra de notificaciones (requerido en Android 13+).

## 📄 Licencia

Este proyecto está distribuido bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

## 👨‍💻 Autor

Desarrollado por **Jhosue Acosta**.
Ingeniero de Sistemas apasionado por la creación de soluciones móviles eficientes y elegantes.

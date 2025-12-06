# Rehabook – Prueba Técnica

Aplicación Android desarrollada en **Kotlin + Jetpack Compose + Firebase Authentication + Firebase Realtime Database**.
Permite registrar usuarios, iniciar sesión y cerrar sesión, guardar y mostrar la información básica del usuario en la pantalla principal. Ahora también incluye la gestión de citas y una funcionalidad de chat en tiempo real con roles de usuario.

---

## Requisitos

*   Android Studio 2022 o superior
*   Dispositivo/emulador con Android 8.0+ recomendado
*   Cuenta Firebase con **Authentication** habilitada (Email/Password)
*   **Realtime Database** activada en tu proyecto Firebase

---

## Configuración inicial

1.  Clonar o descomprimir el proyecto.
2.  Crear un proyecto Android en Firebase.
3.  Descargar `google-services.json` desde Firebase y colocarlo en la raíz del módulo `app/` de tu proyecto.
    > El archivo **no está incluido** en el ZIP entregado.

4.  **Configurar reglas de Firebase Realtime Database:**
    *   En la consola de Firebase, ve a `Realtime Database` y luego a la pestaña `Reglas`.
    *   Actualiza tus reglas con la siguiente configuración para asegurar el acceso adecuado y la gestión de roles para usuarios, citas y chats:

        ```json
        {
          "rules": {
            ".read": "auth != null",
            ".write": "auth != null",
            "usuario": {
              "$uid": {
                ".read": "$uid === auth.uid",
                ".write": "$uid === auth.uid"
              }
            },
            "cita": {
              ".read": "auth != null",
              ".write": "root.child('usuario').child(auth.uid).child('rol').val() == 1"
            },
            "chats": {
              "$chatId": {
                ".read": "auth != null && (data.child('participant1').val() == auth.uid || data.child('participant2').val() == auth.uid)",
                ".write": "auth != null && (data.child('participant1').val() == auth.uid || data.child('participant2').val() == auth.uid)"
              }
            }
          }
        }
        ```
        > **Explicación de las reglas:**
        > *   `.read` y `.write` generales solo permiten acceso a usuarios autenticados.
        > *   El nodo `usuario/$uid` permite que cada usuario (`auth.uid`) solo lea y escriba sus propios datos de perfil.
        > *   El nodo `cita` permite a cualquier usuario autenticado leer las citas. Sin embargo, solo los usuarios con `rol == 1` (administradores/fisioterapeutas en este contexto) pueden crear, editar o eliminar citas (`.write`).
        > *   El nodo `chats/$chatId` permite que los usuarios participantes (`participant1` o `participant2`) en un chat específico lean y escriban mensajes dentro de ese chat.

5.  **Configurar la URL de Realtime Database en la aplicación (importante para bases de datos regionales):**
    *   Asegúrate de que en `MainActivity.kt`, la inicialización de `database` incluya la URL completa de tu instancia de Realtime Database (la puedes encontrar en la consola de Firebase, sección Realtime Database). Ejemplo:
        ```kotlin
        database = Firebase.database("https://rehabook-default-rtdb.europe-west1.firebasedatabase.app").reference
        ```
        > Ajusta `rehabook-default-rtdb.europe-west1.firebasedatabase.app` a la URL exacta de tu base de datos si es diferente.

---

## Cómo ejecutar

1.  Abrir el proyecto en Android Studio.
2.  Realizar una reconstrucción limpia del proyecto (`Build > Clean Project` y `Build > Rebuild Project`).
3.  Ejecutar en un dispositivo/emulador.
4.  Probar la aplicación.

## Credenciales de prueba

1. Admin
*   Email: gmolrod892@gmail.com
*   Contraseña: 123456

2.Usuario
*   Email: tester@rehabook.com
*   Contraseña: 123456

> Puedes crear un nuevo usuario desde la pantalla de registro. Los datos de este usuario también se almacenarán en Realtime Database. Para probar las funcionalidades de administrador (gestión de citas o chat), deberás actualizar manualmente el campo `rol` a `1` en el nodo del usuario en Firebase Realtime Database.

## Flujo de la aplicación

### Login

*   Email y contraseña
*   Botón **Iniciar Sesión**
*   Botón **Crear cuenta**

### Registro

*   Formulario completo:
    *   Nombre
    *   DNI
    *   Email
    *   Teléfono
    *   Contraseña
    *   Confirmar contraseña
*   Botón “Registrar”
    *   **Validaciones:**
        1.  Todos los campos rellenos.
        2.  Email válido (usando patrón `Patterns.EMAIL_ADDRESS` de Android).
        3.  Contraseña con al menos 6 caracteres.
        4.  Teléfono con longitud exacta de 9 caracteres.
        5.  DNI válido (usando método `validarDni`)
        6.  Contraseñas coinciden
    *   Muestra mensajes de error con `Toast` si alguna validación falla.
    *   Al completar correctamente las validaciones, el usuario se registra en Firebase Authentication.
    *   Posteriormente, los datos del usuario (nombre, email, teléfono, DNI y un `rol` predeterminado) se guardan en Firebase Realtime Database bajo una entrada con el UID único del usuario.
    *   Solo después de que ambas operaciones (Authentication y Realtime Database) se confirman como exitosas, la aplicación muestra un `Toast` de "Registro exitoso" y redirecciona automáticamente a la pantalla de Login.
    *   Durante el proceso de registro, la aplicación espera de forma asíncrona a que las operaciones de Firebase se completen, garantizando la persistencia de datos antes de la navegación.

*   Botón **Ya tengo cuenta** para volver al login

### Home

*   Muestra el email del usuario autenticado
*   Botón **Cerrar Sesión**
*   Switch "Mantener sesión iniciada" (opcional, falso por defecto):
    *   Si está activo y cierras la app, al abrirla nuevamente mantendrá la sesión y mostrará Home.
*   **Navegación a la lista de citas** (para usuarios con `rol=1`)
*   **Navegación a la lista de chats** (para usuarios con `rol=1`)

### CitasListScreen (Pantalla de listado de citas)

*   **Propósito:** Muestra un listado de todas las citas guardadas en el sistema y permite su gestión.
*   **Funcionalidad:**
    *   **Lectura desde Firebase:** Se conecta a Firebase Realtime Database y obtiene todas las citas almacenadas bajo el nodo `cita`. La lista se actualiza en tiempo real utilizando un `ValueEventListener`.
    *   **Visualización:** Cada cita se presenta en un `Card` con información básica como Paciente, Motivo, Fisioterapeuta, y fechas de creación/modificación.
    *   **Rol del Usuario:** El rol del usuario autenticado se obtiene de Firebase. Solo los usuarios con `rol = 1` (administradores/fisioterapeutas) tienen permisos para editar o eliminar citas, y para ver el botón de "Agregar Cita".
    *   **Interacción:**
        *   Los administradores pueden tocar una cita para navegar a la pantalla de edición (`CitasFormScreen`) con el `id` correspondiente.
        *   Botones "Editar" y "Eliminar" visibles solo para administradores.
        *   **Eliminación:** Al presionar "Eliminar", un diálogo de confirmación precede a la eliminación de la cita de la base de datos.

### CitasFormScreen (Pantalla para crear o editar citas)

*   **Propósito:** Permite a los usuarios (con `rol = 1`) crear nuevas citas o editar citas existentes.
*   **Funcionalidad:**
    *   **Modo Creación/Edición:**
        *   Si se recibe un `idCita` a través de la navegación, la pantalla carga los detalles de esa cita para su edición.
        *   Si no se recibe `idCita`, la pantalla se inicializa para crear una nueva cita.
    *   **Campos de Entrada:** Los usuarios deben completar los siguientes campos: `Paciente`, `Motivo`, `Diagnóstico`, `Fisioterapeuta`. `Paciente` y `Motivo` son obligatorios.
    *   **Fechas:** La fecha de creación se asigna automáticamente al crear. La fecha de modificación se actualiza al editar.
    *   **Guardar Cita:** Al presionar "Guardar", se validan los campos obligatorios. Si son válidos, la cita se guarda (o actualiza) en el nodo `cita` de Firebase Realtime Database.
    *   **Carga Visual:** Se muestra un indicador de carga mientras se espera la respuesta de Firebase.

### ChatListScreen (Pantalla de lista de chats)

*   **Propósito:** Permite a los administradores (`rol = 1`) ver y gestionar todas las conversaciones activas con los usuarios.
*   **Funcionalidad:**
    *   **Lista dinámica:** Muestra un listado de chats con el nombre/correo del usuario, último mensaje y fecha. Se actualiza en tiempo real con `ChildEventListener`.
    *   **Búsqueda y Ordenación:** Incluye un buscador por usuario/correo y opciones para ordenar por fecha de mensaje (más recientes/antiguos).
    *   **Visibilidad de mensajes:** Muestra el último mensaje y marca los chats con mensajes no leídos para el administrador.
    *   **Roles:** Solo accesible y funcional para administradores.

### ChatScreen (Pantalla de conversación individual)

*   **Propósito:** Facilita la comunicación en tiempo real entre un administrador y un usuario específico.
*   **Funcionalidad:**
    *   **Visualización de mensajes:** Muestra un flujo de mensajes en tiempo real, con un campo de entrada para enviar nuevos mensajes.
    *   **Actualizaciones en tiempo real:** Utiliza `ChildEventListener` para detectar y mostrar nuevos mensajes instantáneamente.
    *   **Lectura automática:** Marca automáticamente los mensajes como leídos para el usuario (o administrador) que los visualiza, actualizando los campos `lastRead_user` o `lastRead_admin` en Firebase.
    *   **Almacenamiento:** Los mensajes se guardan en Firebase Realtime Database bajo la estructura `/chats/{chatId}/mensajes/{mensajeId}`.

### Cerrar sesión

*   Se cierra la sesión de **FirebaseAuth**.
*   Vuelve automáticamente a la pantalla de **Login**.

---
3.  ¿Qué otras medidas de seguridad podríamos aplicar para el acceso a los nodos de `usuario`, `cita` y `chats`, especialmente si el `rol` del usuario debe ser gestionado por un sistema backend seguro en lugar de directamente en la base de datos?

# Rehabook – Prueba Técnica

Aplicación Android desarrollada en **Kotlin + Jetpack Compose + Firebase Authentication**.  
Permite registrar usuarios, iniciar sesión y cerrar sesión, mostrando la información básica en la pantalla principal.

---

## Requisitos

- Android Studio 2022 o superior
- Dispositivo/emulador con Android 8.0+ recomendado
- Cuenta Firebase con Authentication habilitada (Email/Password)
- Realtime Database activada en tu proyecto Firebase

---

## Configuración inicial

1. Clonar o descomprimir el proyecto.
2. Crear un proyecto Android en Firebase.
3. Descargar google-services.json desde Firebase y colocarlo en la raíz del módulo app/ de tu proyecto.
> El archivo **no está incluido** en el ZIP entregado.
4.Configurar reglas de Firebase Realtime Database:
    - En la consola de Firebase, ve a Realtime Database y luego a la pestaña Reglas .
    - Para desarrollo inicial y pruebas, puedes usar reglas permisivas (¡ no usar en producción! ):
    > {
      "rules": {
        ".read": "true",
        ".write": "true"
      }
    }
    - Para producción, se recomienda una configuración más segura, por ejemplo, permitiendo que cada usuario solo lea y escriba sus propios datos:
    > {
      "rules": {
        "usuarios": {
          "$uid": {
            ".read": "$uid === auth.uid",
            ".write": "$uid === auth.uid"
          }
        }
      }
    }
5. Configurar la URL de Realtime Database en la aplicación (importante para bases de datos regionales):
    - Asegúrate de que en MainActivity.kt , la inicialización de database incluya la URL completa de tu instancia de Realtime Database (la puedes encontrar en la consola de Firebase, sección Realtime Database). Ejemplo:
> database = Firebase.database("https://rehabook-default-rtdb.europe-west1.firebasedatabase.app").reference



## Cómo ejecutar
1. Abrir el proyecto en Android Studio.
2. Realizar una reconstrucción limpia del proyecto ( Build > Clean Project y Build > Rebuild Project )
3. Ejecutar en un dispositivo/emulador.
4. Probar la aplicación.

## Credenciales de prueba
- Email: usuario@usuario.com
- Contraseña: 123456

> Puedes crear un nuevo usuario desde la pantalla de registro. Los datos de este usuario también se almacenarán en Realtime Database.

## Flujo de la aplicación

### Login
- Email y contraseña
- Botón **Iniciar Sesión**
- Botón **Crear cuenta**

### Registro
- Formulario completo:
    - Nombre
    - DNI
    - Email
    - Teléfono
    - Contraseña
- Botón “Registrar”
  - Validaciones:
        1.Todos los campos rellenos.
        2.Email válido (usando patrón `Patterns.EMAIL_ADDRESS` de Android).
        3.Contraseña con al menos 6 caracteres.
        4.Teléfono con longitud exacta de 9 caracteres.
        5.DNI válido (usando método validarDni)
  - Muestra mensajes de error con Toast si alguna validación falla.
  - Al completar correctamente las validaciones, el usuario se registra en Firebase Authentication.
  - Posteriormente, los datos del usuario (nombre, email, teléfono, DNI) se guardan en Firebase Realtime Database bajo una entrada con el UID único del usuario.
  - Solo después de que ambas operaciones (Authentication y Realtime Database) se confirman como exitosas , la aplicación muestra un Toast de "Registro exitoso" y redirecciona automáticamente a la pantalla de Login.
  - Durante el proceso de registro, la aplicación espera de forma asíncrona a que las operaciones de Firebase se completen, garantizando la persistencia de datos antes de la navegación.

- Botón **Ya tengo cuenta** para volver al login

### Home
- Muestra el email del usuario autenticado
- Botón **Cerrar Sesión**
- Switch "Mantener sesión iniciada" (opcional, falso por defecto):
    - Si está activo y cierras la app, al abrirla nuevamente mantendrá la sesión y mostrará Home.  

### Cerrar sesión
- Se cierra FirebaseAuth.
- Vuelve automáticamente a la pantalla de **Login**.

---

**Fin del README**

# Rehabook – Prueba Técnica

Aplicación Android desarrollada en **Kotlin + Jetpack Compose + Firebase Authentication**.  
Permite registrar usuarios, iniciar sesión y cerrar sesión, mostrando la información básica en la pantalla principal.

---

## Requisitos

- Android Studio 2022 o superior
- Dispositivo/emulador con Android 8.0+ recomendado
- Cuenta Firebase con Authentication habilitada (Email/Password)

---

## Configuración inicial

1. Clonar o descomprimir el proyecto.
2. Crear un proyecto Android en Firebase.
3. Descargar `google-services.json` desde Firebase y colocarlo en: app/google-services.json

> El archivo **no está incluido** en el ZIP entregado.

## Cómo ejecutar
1. Abrir el proyecto en Android Studio.
2. Ejecutar en un dispositivo/emulador.
3. Probar la aplicación.

## Credenciales de prueba
- Email: prueba@demo.com
- Contraseña: 123456

> Puedes crear un nuevo usuario desde la pantalla de registro.

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
  - Al completar correctamente, registra al usuario y redirige al login.

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

# Flores Hermanos

Sistema académico de gestión de pasajes, embarque y fidelización. Implementado en Java con Spring Boot, MVC, servicios, DAO JDBC y PostgreSQL.

## Funciones

- Venta y reserva temporal de asientos, con promociones y canje de puntos.
- Boletos QR, validación de embarque y manifiestos.
- Gestión de pasajeros, rutas, viajes, buses, conductores y cuentas.
- Reportes, auditoría y respaldo PostgreSQL.

Los datos iniciales son ficticios. Los pagos y el envío a SUTRAN se simulan.

## Ejecutar en Windows

1. Instala un JDK 17 o superior, Maven y PostgreSQL 15 o superior.
2. Ejecuta `COMPILAR.bat` para generar el archivo JAR.
3. Ejecuta `INICIAR.bat`. El iniciador crea un clúster PostgreSQL local para este proyecto y abre la aplicación en `http://127.0.0.1:8080`.
4. Usa la cuenta de demostración `admin` con la clave inicial `Flores2026!`.

También puedes usar una base PostgreSQL vacía existente. Configura las variables `FLORES_DB_URL`, `FLORES_DB_USER` y `FLORES_DB_PASSWORD` y ejecuta `java -jar flores-hermanos.jar`. Conserva la configuración local fuera del control de versiones.

## Código y pruebas

- `codigo/` contiene el proyecto Maven, el esquema SQL y las pruebas de integración.
- `diagramas/` contiene las fuentes PlantUML del diseño.
- `PRUEBAS.ps1` ejecuta la suite con una base de prueba separada. Mantén abierta la aplicación iniciada con `INICIAR.bat` para que el script pueda reutilizar el clúster local.

Proyecto académico de Axel Palomino y Piero Franco.

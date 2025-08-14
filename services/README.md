# 🛡️ shared.security

Módulo **Spring Security** común para proteger microservicios con autenticación **JWT RS256** emitida por `ms-auth`.

Su objetivo es:
- Evitar duplicar configuración de seguridad en cada microservicio.
- Centralizar el manejo de JWT y CORS.
- Asegurar que todas las rutas estén protegidas por defecto.
- Permitir definir endpoints públicos (health checks, login, etc.).

---

## 📂 Contenido del módulo

Incluye:
- **`MSSecurityConfig`** → Configuración común de seguridad para todos los microservicios.
- **`MSJwtAuthFilter`** → Filtro que valida tokens JWT con clave pública.
- Carga automática de la clave pública (`public.pem`) desde recurso o ruta configurada.
- Configuración de **CORS** para permitir el acceso desde el frontend.
- Lista configurable de endpoints **permitAll** en `application.yml`.

---

## 🚀 Pasos para proteger un nuevo microservicio

### 1️⃣ Compilar e instalar `shared.security` en el repositorio local
En la carpeta `services/shared.security` ejecutar:
```bash
mvn clean install

### 2️⃣ Añadir dependencia en pom.xml

En el microservicio que quieras proteger:
<dependency>
    <groupId>com.oscar</groupId>
    <artifactId>shared.security</artifactId>
    <version>1.0.0</version>
</dependency>

3️⃣ Añadir la clave pública

Crear el archivo:
src/main/resources/jwt/public.pem

Copiar dentro la clave pública correspondiente a la privada usada por ms-auth para firmar JWTs.
Formato esperado:
-----BEGIN PUBLIC KEY-----
...clave en base64...
-----END PUBLIC KEY-----


4️⃣ Configurar application.yml

Añadir:

app:
  jwt:
    issuer: ms-auth
    public-key-location: classpath:jwt/public.pem

security:
  permit-all: /actuator/health,/actuator/info

cors:
  allowed-origins: ""
  allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
  allowed-headers: "*"


issuer: Debe coincidir con el valor configurado en ms-auth.
permit-all: Lista de endpoints que estarán accesibles sin autenticación.
cors.allowed-origins: Origen del frontend en desarrollo.


5️⃣ Importar MSSecurityConfig en la clase principal del microservicio

Añadir la anotación en la clase @SpringBootApplication principal del microservicio:
import com.oscar.shared.security.MSSecurityConfig;

Con este simple import, Spring Boot cargará automáticamente la configuración de seguridad común.


6️⃣ Verificación del funcionamiento

Arrancar el microservicio.
Acceder a un endpoint protegido sin token → Debe devolver 401 Unauthorized.
Generar un JWT válido desde ms-auth y repetir la petición con el header:
Authorization: Bearer <token>
→ Debe devolver la respuesta correcta (200 OK).

Acceder a un endpoint definido en permit-all → Debe responder sin autenticación.


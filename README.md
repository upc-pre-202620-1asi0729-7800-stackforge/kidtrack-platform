# KidTrack Platform

Backend de **KidTrack**, la plataforma web de movilidad escolar de StackForge. Es la migración a **Java + Spring Boot**
del backend de SaferRoute (`saferoute-powertech-platform`, ASP.NET Core), adaptada al dominio y a la aplicación web de
KidTrack (`kidtrack-webapp`).

## Stack

| Pieza | Tecnología |
|---|---|
| Lenguaje / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Security, Actuator) |
| Base de datos | MySQL 8 (H2 en memoria para las pruebas) |
| Seguridad | JWT HS256 firmado con Spring Security OAuth2 Resource Server (Nimbus) y BCrypt |
| Documentación | Swagger UI / OpenAPI (springdoc) |
| Contenedores | Dockerfile multi-etapa + `docker-compose.yml` |

## Arquitectura

Monolito modular con **Domain-Driven Design**: seis bounded contexts más un *shared kernel*. Cada contexto tiene las
mismas capas que el backend original:

```
com.stackforge.kidtrack.platform
├── shared          Value objects (FullName, Email, Coordinates), excepciones, seguridad JWT, CORS, OpenAPI
├── iam             Usuarios, organizaciones, sign-in / sign-up, tokens
├── subscription    Planes y suscripciones de las organizaciones
├── stakeholder     Padres, alumnos y conductores
├── fleet           Vehículos y rutas (paradas, conductor, vehículo, alumnos)
├── trip            Viajes (programado → en ruta → completado) e incidentes
└── notifications   Avisos a los padres
    └── cada contexto:
        domain/model/{aggregates,entities,valueobjects,commands,queries,events}
        domain/services              interfaces de los servicios de comando y de consulta
        application/internal         implementaciones (commandservices, queryservices, eventhandlers)
        infrastructure/persistence   repositorios JPA y seeders
        interfaces/rest              controladores, resources y assemblers
        interfaces/acl               fachadas que exponen datos a otros contextos
```

Reglas de dependencia entre contextos:

- Los contextos solo se consultan a través de **fachadas ACL** (`interfaces/acl`): `IamContextFacade`,
  `StakeholderContextFacade`, `SubscriptionContextFacade` y `FleetContextFacade`.
- Se comunican por **eventos de dominio** síncronos (Spring `ApplicationEventPublisher`): al actualizar una ruta, sus
  viajes abiertos siguen al nuevo conductor y vehículo (`RouteConfigurationChanged`); al borrar una ruta se borran sus
  viajes e incidentes (`RouteDeleted`), y al borrar un viaje se borran sus notificaciones (`TripDeleted`).

## Contrato REST (compatible con `kidtrack-webapp`)

El API replica el contrato de la *Fake API* (json-server) que consume el front: mismos paths, mismos campos y los
mismos filtros por query string. Para usarlo basta con apuntar el front a este backend:

```ts
// kidtrack-webapp/src/environments/environment.ts
apiBaseUrl: 'http://localhost:8080/api/v1',
```

| Contexto | Endpoints (`/api/v1/...`) |
|---|---|
| IAM | `POST /authentication/sign-in`, `POST /authentication/sign-up`, `GET /users?organizationId=`, `GET·PATCH·PUT /users/{id}`, `POST /organizations`, `GET·PATCH·PUT /organizations/{id}` |
| Subscription | `GET /plans`, `GET /plans/{id}`, `POST /plans`, `GET /subscriptions?organizationId=&state=`, `POST /subscriptions`, `GET·PATCH·PUT /subscriptions/{id}` |
| Stakeholder | `/parents`, `/children?organizationId=&parentId=`, `/profiles?organizationId=&role=driver` (todos con `GET`, `POST`, `PUT`, `PATCH`, `DELETE`) |
| Fleet | `/vehicles` y `/routes` (`GET`, `POST`, `PUT`, `DELETE`; las rutas incluyen `waypoints`) |
| Trip | `/trips` (`GET`, `POST`, `PATCH`, `DELETE`) e `/incidents` (`GET`, `POST`, `PATCH`) |
| Notifications | `/notifications?organizationId=&parentId=&tripId=&read=` (`GET`, `POST`, `PATCH`, `DELETE`) |

Todos los `GET` de colecciones aceptan `organizationId`; si no se envía se usa la organización del token. La
documentación completa está en Swagger UI: `http://localhost:8080/swagger-ui.html`.

### Seguridad

- Son públicos: `POST /authentication/**`, `POST /organizations` (la organización se crea antes que su primer
  administrador), `GET /plans`, Swagger y `/actuator/health`. Todo lo demás requiere `Authorization: Bearer <token>`.
- El token lleva `sub` (id del usuario), `role` (`ADMIN`, `DRIVER`, `PARENT`) y `organizationId`.
- **Multi-organización**: cada usuario solo ve y modifica datos de su organización.
- **Registro**: el primer administrador de una organización se registra sin token; los conductores, padres y demás
  administradores los registra un administrador de esa organización.
- **Permisos de escritura**: los administradores gestionan padres, alumnos, conductores, vehículos, rutas, viajes y
  suscripciones. Los conductores pueden actualizar *su* viaje, marcar el abordaje de los alumnos, reportar incidentes
  y enviar avisos.

## Reglas de dominio

- Un viaje sigue `SCHEDULED → EN_ROUTE → COMPLETED` (o `CANCELLED`); no puede haber dos viajes de la misma ruta, día y
  hora, y solo se programan sobre rutas activas con conductor.
- Una organización tiene como máximo una suscripción activa: suscribirse a un plan cancela la anterior. Una suscripción
  activa cuya fecha de fin pasó se informa como `EXPIRED`.
- Las cuotas del plan activo (rutas, conductores y alumnos) se validan al crear rutas, conductores y alumnos.
- Los vehículos tienen placa única por organización; al borrar uno, las rutas que lo usaban quedan sin vehículo.
- Al borrar un padre, sus alumnos se conservan sin padre.

## Cómo ejecutarlo

### Con Docker (recomendado)

```bash
docker compose up --build
```

Levanta MySQL y el API en `http://localhost:8080`. Con `SEED_ENABLED=true` (por defecto) carga datos de demostración
en la base vacía.

### Con Maven

Requiere Java 21, Maven 3.9 y una base MySQL (por defecto `localhost:3306`, base `kidtrack_platform`).

```bash
mvn spring-boot:run
mvn test        # pruebas de integración con H2, no necesitan MySQL
```

### Perfiles y variables de entorno

La configuración está en `src/main/resources`: `application.properties` (lo común), `application-dev.properties`
(desarrollo local, perfil por defecto) y `application-prod.properties` (producción). El perfil se elige con
`SPRING_PROFILES_ACTIVE`.

| Variable | Descripción | `dev` | `prod` |
|---|---|---|---|
| `DATABASE_URL` | Host de MySQL (solo el host, no una URL JDBC) | `localhost` | obligatoria |
| `DATABASE_PORT` | Puerto de MySQL | `3306` | `3306` |
| `DATABASE_NAME` | Nombre de la base | `kidtrack_platform` | obligatoria |
| `DATABASE_USERNAME` | Usuario de la base | `root` | obligatoria |
| `DATABASE_PASSWORD` | Contraseña de la base | vacía (defínela) | obligatoria |
| `DATABASE_SSL` | Usa SSL con MySQL | `false` | `true` |
| `JWT_SECRET` | Secreto de los tokens (mínimo 32 caracteres) | valor solo para desarrollo | obligatoria |
| `JWT_EXPIRATION_DAYS` | Vigencia del token en días | `7` | `7` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos, separados por coma (vacío = cualquiera) | vacío | vacío |
| `SEED_ENABLED` | Carga los datos de demostración | `true` | `false` |
| `SWAGGER_ENABLED` | Publica Swagger UI | siempre activo | `false` |
| `PORT` | Puerto HTTP | `8080` | `8080` |

## Datos de demostración

Organización *Transportes Escolares Lima Norte S.A.C.* con plan Completo activo, dos rutas, dos vehículos, tres alumnos y
sus viajes. Cuentas:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@kidtrack.pe` | `admin123` |
| Conductor | `driver@kidtrack.pe` | `driver123` |
| Padre | `parent@kidtrack.pe` | `parent123` |

Son solo para desarrollo: desactiva `SEED_ENABLED` en producción.

## Qué cambió respecto al backend de SaferRoute

| SaferRoute (.NET) | KidTrack Platform (Spring Boot) |
|---|---|
| ASP.NET Core 10 + EF Core + MySQL | Spring Boot 3.5 + Spring Data JPA + MySQL |
| Cortex.Mediator (comandos y consultas) | Servicios de comando y de consulta con records de `Command` / `Query` |
| `Result<T>` + `ProblemDetailsFactory` | Excepciones de dominio + `GlobalExceptionHandler` (RFC 7807) |
| JWT con `JwtBearer` | JWT con Spring Security OAuth2 Resource Server |
| BCrypt.Net | `BCryptPasswordEncoder` |
| Swashbuckle | springdoc-openapi |
| Migraciones EF y `Database.Migrate()` | `spring.jpa.hibernate.ddl-auto=update` |
| Seeders al arrancar | `CommandLineRunner` por contexto (`kidtrack.seed.enabled`) |
| API propio (`/users/sign-in`, recursos anidados) | API con el contrato del front de KidTrack (`/authentication/sign-in`, recursos planos) |

Adaptaciones al dominio de KidTrack:

- Se mantienen los seis bounded contexts y su estructura. El contexto *Stakeholder* pasa a tener alumnos (`Child`) como
  recurso propio, con grado, estado de abordaje y foto.
- *Fleet* modela la ruta como en el front (tipo `OUTBOUND`/`RETURN`, paradas con coordenadas y alumnos, horario `HH:mm`)
  en lugar del ciclo borrador/activa de SaferRoute.
- *Trip* reemplaza `PENDING/IN_PROGRESS/COMPLETED` por los estados del front (`SCHEDULED`, `EN_ROUTE`, `COMPLETED`,
  `CANCELLED`) y guarda con el viaje el nombre de la ruta, el conductor y el vehículo.
- *Notifications* usa los tipos de aviso del front (`ABORDAJE`, `PROXIMIDAD`, `AUSENCIA`, `LLEGADA`, `RETRASO`) y el
  campo `read`.
- Los identificadores de dominio son `UUID` directos (sin clases `*Id` envolventes).
- No se migraron los grupos de alumnos (`StudentGroup`) ni las alertas y anuncios de SaferRoute, porque KidTrack no los
  usa; los avisos a los padres cubren ese caso.

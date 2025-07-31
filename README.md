# SimulaciónProduccion

## Resumen del proyecto
Simulación completa de un servicio tecnológico en una producción de metal, basado en arquitectura de microservicios.  
Cada microservicio gestiona un dominio concreto y se orquesta para replicar un flujo de producción industrial.

### Microservicios incluidos
- **Auth**: Autenticación y autorización (JWT, OAuth2).  
- **Recursos Humanos**: Gestión de empleados, turnos y nóminas.  
- **Compras y Proveedores**: Órdenes de compra, catálogos y relaciones con proveedores.  
- **Producción**: Control de órdenes de fabricación y ejecución de procesos.  
- **Inventario y Trazabilidad**: Stock de materiales y localización de lotes.  
- **Pasarela y Orquestación**: API Gateway y ruteo de peticiones.  
- **Incidencias**: Registro y seguimiento de fallos y averías.  
- **Mantenimiento**: Planificación preventivo/correctivo de máquinas.  
- **Control de Calidad**: Inspección de piezas, pruebas y certificaciones.  
- **Planificación Avanzada (MRP/ERP)**: Algoritmos de planificación y optimización.  
- **Logística y Distribución**: Gestión de envíos, transporte y rutas.  
- **Finanzas y Facturación**: Costeo, facturación automática y contabilidad.  
- **Analítica y BI**: Dashboards, métricas y KPIs de producción.  
- **Gestión de Energía y Recursos**: Monitoreo de consumos y eficiencia.  
- **Gestión Ambiental y Residuos**: Emisiones, tratamiento de residuos y cumplimiento normativo.  
- **Gestión Documental y Conformidad**: Manuales, auditorías y control de versiones de documentos.

---

## Bounded contexts
Cada microservicio representa un **contexto delimitado** con su propio modelo y datos:

| Contexto                                  | Descripción breve                                           |
|-------------------------------------------|-------------------------------------------------------------|
| Auth                                      | Usuarios, roles, permisos y tokens JWT                      |
| Recursos Humanos                          | Empleados, contratos, turnos y nóminas                      |
| Compras y Proveedores                     | Proveedores, órdenes de compra y catálogos                  |
| Producción                                | Órdenes de fabricación y flujo de procesos                  |
| Inventario y Trazabilidad                 | Gestión de stock y tracking de lotes                        |
| Pasarela y Orquestación                   | Enrutamiento de APIs y seguridad de borde                   |
| Incidencias                               | Registro, priorización y seguimiento de fallos              |
| Mantenimiento                             | Planificación de tareas preventivas y reparaciones          |
| Control de Calidad                        | Tests de calidad, auditorías y certificaciones              |
| Planificación Avanzada (MRP/ERP)          | Algoritmos MRP y optimización de recursos                   |
| Logística y Distribución                  | Gestión de envíos, rutas y transportistas                   |
| Finanzas y Facturación                    | Facturación, contabilidad y análisis de costes              |
| Analítica y BI                            | KPIs, dashboards y reporting                                |
| Gestión de Energía y Recursos             | Monitoreo y control de consumos                             |
| Gestión Ambiental y Residuos              | Emisiones, reciclaje y conformidad legal                    |
| Gestión Documental y Conformidad          | Almacenamiento, versiones y auditorías de documentos        |

---

## Tipo de comunicación
- **REST (HTTP síncrono):**  
  Para operaciones CRUD y llamadas directas: simple de usar y depurar.  
- **Mensajería (eventos asíncronos):**  
  Para flujos de eventos de dominio, desacoplamiento y escalabilidad (Kafka/RabbitMQ).

---

## Modelo de branching
- La rama principal (`main`) se utiliza exclusivamente para **despliegue automático** en Vercel.  
  Cada push a `main` desencadena un despliegue en producción.  
- La rama `develop` se emplea para **desarrollo diario**: nuevas funcionalidades y correcciones.  
- Para cada feature o hotfix, crea una rama corta desde `develop` y, una vez lista, mézclala de nuevo en `develop` vía pull request.  
- Cuando `develop` esté estable y listo para release, se fusiona en `main` para publicar en producción.  

---

## Versionado de APIs
- **Versionado en ruta**:  
  Cada servicio expone sus endpoints bajo `/api/v{MAJOR}/…`  
  - v1: primera versión estable  
  - v2: cambios incompatibles  
- Documentación y fechas de obsolescencia asociadas a cada versión.

---



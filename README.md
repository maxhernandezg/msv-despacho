# Microservicio Despacho — Despliegue Cloud-Native en AWS EKS

Microservicio **despacho** (Spring Boot) del sistema de gestión de despachos de
**Innovatech**, desplegado en un clúster **Kubernetes (AWS EKS)** con pipeline
**CI/CD** automatizado y una capa **serverless** (Lambda + SQS + API Gateway).

---

## Equipo

| Integrante | Usuario de GitHub | Rol en el encargo |
|------------|-------------------|-------------------|
| Maximiliano Hernandez Gallardo | [@maxhernandezg](https://github.com/maxhernandezg) | Configuracion del repositorio, pipeline de CI/CD y revision de PRs |
| Marcos del Canto Vargas | [@MarcosdelCanto](https://github.com/MarcosdelCanto) | Documentacion de convenciones, pruebas y revision de PRs |

Ambos integrantes figuran como *code owners* del repositorio
(`.github/CODEOWNERS`), por lo que GitHub solicita automaticamente su
revision en cada Pull Request.

Asignatura **DOY0101 — Ingenieria DevOps** · Evaluacion Parcial N°1 ·
Repositorio: `maxhernandezg/msv-despacho`

---

## Estrategia de ramificacion

### Modelos evaluados

| Modelo | Como funciona | Donde rinde mejor | Limitaciones |
|--------|---------------|-------------------|--------------|
| **Git Flow** | Dos ramas permanentes (`main` y `develop`) y ramas temporales `feature/`, `release/` y `hotfix/`. | Equipos con versiones identificables y despliegues planificados; separa con claridad lo que esta en produccion de lo que esta en desarrollo. Permite corregir produccion (`hotfix/`) sin arrastrar trabajo a medio terminar. | Mas ramas que administrar; si las `feature/` viven mucho tiempo aparecen conflictos grandes al integrar. |
| **GitHub Flow** | Una sola rama permanente (`main`) y ramas cortas por cambio, integradas por Pull Request y desplegadas de inmediato. | Aplicaciones web y servicios en la nube con despliegue continuo varias veces al dia. | No distingue "listo para probar" de "listo para produccion": todo lo integrado esta publicado. Exige alta cobertura de pruebas automatizadas. |
| **Trunk-Based Development** | Todo el equipo integra al tronco (`main`) al menos una vez al dia, con ramas de muy corta vida y *feature flags* para lo inconcluso. | Equipos maduros con integracion continua estricta, despliegues automatizados y pruebas confiables. Minimiza el costo de integracion. | Requiere disciplina y automatizacion alta; sin ellas se rompe la rama principal con facilidad. |

### Modelo adoptado: Git Flow

Este repositorio implementa **Git Flow** con las ramas `main`, `develop`,
`feature/*` y `hotfix/*`. Los criterios de la decision fueron:

1. **Separacion entre produccion y desarrollo.** `main` refleja lo que esta
   corriendo en el cluster EKS: cada push a esa rama dispara el despliegue real
   (`deploy-eks.yml`). Contar con `develop` permite integrar y probar trabajo
   del equipo sin tocar el ambiente desplegado.
2. **Trabajo en paralelo de dos personas.** Cada integrante desarrolla en su
   propia rama `feature/` y la integracion ocurre en un punto unico y revisado
   (el Pull Request hacia `develop`), lo que evita pisar el trabajo del otro.
3. **Correcciones urgentes sin arrastrar cambios.** Una rama `hotfix/` nace de
   `main`, se corrige el defecto y vuelve a `main` y a `develop`, sin publicar
   funcionalidades a medio terminar que esten en `develop`.
4. **Trazabilidad exigida por el encargo.** Los merge commit (`--no-ff`) dejan
   en el historial el punto exacto de integracion de cada Pull Request, con su
   revision y el resultado del pipeline asociado.

Se descarto **trunk-based development** porque el equipo despliega sobre un
laboratorio AWS Academy cuyas credenciales son temporales y la cadencia de
integracion no es diaria; y **GitHub Flow** porque no ofrece una rama de
integracion previa a produccion, que es justamente la que permite acumular y
validar varios cambios antes de desplegar.

### Ramas del repositorio

| Rama | Proposito | Nace de | Se integra a | Automatizacion asociada |
|------|-----------|---------|--------------|-------------------------|
| `main` | Codigo en produccion (desplegado en EKS) | — | — | `CD` (build, push a ECR, deploy a EKS) |
| `develop` | Integracion del trabajo del equipo | `main` | `main` | `CI` en cada push |
| `feature/pipeline-ci` | Pipeline de integracion continua | `develop` | `develop` | `CI` en el Pull Request |
| `feature/documentacion-git` | Convenciones y guia de buenas practicas | `develop` | `develop` | `CI` en el Pull Request |
| `hotfix/location-header-post` | Correccion urgente del encabezado `Location` | `main` | `main` y `develop` | `CI` en el PR + `CD` al integrar |

```
main       ──●──────────────────────────────●───────────────●──────► (produccion / EKS)
              \                            /↑              /↑
               \                          /  \            /  \
develop     ────●────●────●──────────────●─────\──────────●    \    (integracion)
                 ↑    ↑                          \             \
      feature/pipeline-ci  feature/documentacion-git    hotfix/location-header-post
```

### Convenciones de trabajo

El detalle completo (naming de ramas, mensajes de commit, estructura de
carpetas, control de versiones, flujo de merge y estrategia de revision) esta
en **[CONTRIBUTING.md](CONTRIBUTING.md)**. En resumen:

- **Ramas:** `feature/<nombre-corto>` y `hotfix/<nombre-corto>`, en minusculas
  y separadas por guion medio.
- **Commits:** Conventional Commits — `feat`, `fix`, `docs`, `test`, `ci`,
  `build`, `refactor`, `chore` — en imperativo y con un cambio logico por commit.
- **Merge:** solo por Pull Request, con merge commit (`--no-ff`), CI en verde y
  revision cruzada del otro integrante. Sin `push` directo a `main` ni `develop`.
- **Revision:** se verifica que el CI pase, que el cambio corresponda al titulo
  del PR, que se respeten las convenciones y que no se filtren credenciales ni
  artefactos generados.

La trazabilidad del flujo colaborativo (comandos ejecutados y Pull Requests
integrados) esta documentada en **[docs/bitacora-git.md](docs/bitacora-git.md)**.

---

## Automatizacion: rol de GitHub Actions en el CI/CD

El repositorio tiene dos workflows con responsabilidades separadas:

| Workflow | Archivo | Se dispara con | Que hace | Etapa |
|----------|---------|----------------|----------|-------|
| **CI - despacho** | `.github/workflows/ci.yml` | push a `develop`, pull request a `main` y a `develop` | Compila con JDK 17, ejecuta las pruebas (`./mvnw verify`), publica el reporte como artefacto y valida la construccion de la imagen Docker | Integracion continua |
| **CD - despacho** | `.github/workflows/deploy-eks.yml` | push a `main` (merge de un PR) | Construye la imagen, la publica en Amazon ECR etiquetada con el SHA del commit y actualiza el Deployment en el cluster EKS | Entrega continua |

**Rol en el proceso CI/CD.** El workflow de CI es la barrera de calidad: se
ejecuta en un runner efimero de GitHub (entorno limpio en la nube), por lo que
detecta errores que en la maquina de un integrante pasarian inadvertidos —
dependencias faltantes, configuracion que solo existe en local o pruebas que
dependen de una base de datos real. Al estar asociado al Pull Request, el
resultado del pipeline es parte de la revision: si el check esta rojo, el
cambio no se integra. El workflow de CD toma el codigo ya revisado e integrado
en `main` y lo lleva al cluster, cerrando el ciclo desde el commit hasta el
ambiente desplegado sin intervencion manual.

> El job de despliegue esta condicionado a la variable de repositorio
> `DEPLOY_EKS` (*Settings → Secrets and variables → Actions → Variables*).
> Con `DEPLOY_EKS=true` y las credenciales del Learner Lab vigentes
> (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) el
> despliegue se ejecuta; en caso contrario se omite y el pipeline no falla.

---

## Declaracion de uso de Inteligencia Artificial

Conforme a las indicaciones de la evaluacion y a la politica institucional
(<https://bibliotecas.duoc.cl/ia>), se declara:

- **Herramienta utilizada:** Claude (Anthropic), asistente de codigo.
- **Uso dado:** apoyo en la redaccion y formato de la documentacion tecnica
  (este README y `CONTRIBUTING.md`), configuracion de los archivos YAML de
  GitHub Actions y revision de la estructura del repositorio.
- **Validacion:** el equipo reviso, ejecuto y verifico todo el contenido
  generado: los workflows fueron ejecutados en GitHub Actions y las pruebas
  corridas localmente antes de integrarse.
- **Sin apoyo de IA:** la eleccion del modelo de ramificacion, las
  justificaciones tecnicas y las reflexiones individuales de la seccion
  siguiente son elaboracion propia del equipo.

## Conclusiones y reflexiones individuales

<!-- Esta seccion debe ser redactada por cada integrante SIN apoyo de IA,
     segun lo exige la pauta de la evaluacion. Incluir aprendizaje y
     contribucion personal al proyecto. -->

**Maximiliano Hernandez Gallardo:**
_(completar)_

**Marcos del Canto Vargas:**
_(completar)_

---

## 🏗️ Arquitectura

```
                          Internet
                             │
              ┌──────────────┴───────────────┐
              ▼                               ▼
   ┌────────────────────┐          ┌─────────────────────┐
   │  AWS Load Balancer │          │   API Gateway (HTTP) │
   │  (URL pública)     │          │   POST /despacho     │
   └─────────┬──────────┘          └──────────┬──────────┘
             ▼                                 ▼
   ┌────────────────────┐          ┌─────────────────────┐
   │  Front (nginx)     │          │ Lambda productor    │
   │  reverse proxy     │          └──────────┬──────────┘
   └─────────┬──────────┘                     ▼
             ▼                        ┌─────────────────┐
   ┌─────────────────────┐           │   Cola SQS      │
   │ venta-service       │           └────────┬────────┘
   │ despacho-service    │                    ▼
   │   (microservicios)  │           ┌─────────────────────┐
   └─────────┬───────────┘           │ Lambda consumidor   │
             ▼                        └─────────────────────┘
   ┌─────────────────────┐
   │ MySQL (pod)         │
   └─────────────────────┘
       (todo en EKS)
```

- **Clúster:** AWS EKS (`innovatech-eks`), Kubernetes 1.35.
- **Microservicios:** `despacho` (8081) y `venta` (8080), Spring Boot.
- **Base de datos:** MySQL como pod dentro del clúster.
- **Frontend:** React + nginx (reverse proxy a los microservicios).
- **Serverless:** ver [`serverless/README.md`](serverless/README.md).

---

## 🛠️ Tecnologías

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 17 (Spring Boot) |
| Contenedores | Docker (multi-stage) |
| Orquestación | Kubernetes — AWS EKS |
| Registro de imágenes | Amazon ECR |
| CI/CD | GitHub Actions |
| Serverless | AWS Lambda, SQS, API Gateway |
| Observabilidad | CloudWatch Container Insights |

---

## 📂 Estructura del repositorio

```
msv-despacho/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml          # CI: build + pruebas (push a develop, PR a main)
│   │   └── deploy-eks.yml  # CD: build → push ECR → deploy EKS (push a main)
│   ├── ISSUE_TEMPLATE/     # Plantillas de issues
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── CODEOWNERS          # Revisores automaticos de cada PR
├── docs/
│   └── bitacora-git.md     # Trazabilidad del flujo colaborativo
├── k8s/
│   └── despacho.yaml       # Deployment + Service + HPA
├── serverless/             # Capa serverless (Lambda + SQS + API Gateway)
│   ├── provision.sh        # IaC: provisiona todo (idempotente)
│   ├── teardown.sh         # Borra la capa serverless
│   ├── productor-despacho/
│   └── consumidor-despacho/
├── src/
│   ├── main/               # Código fuente Spring Boot
│   └── test/               # Pruebas automatizadas (H2 en memoria)
├── .gitignore
├── CONTRIBUTING.md         # Guia de buenas practicas del repositorio
├── Dockerfile              # Imagen del microservicio (multi-stage, Maven + JRE)
├── mvnw / .mvn/            # Maven Wrapper (misma version de Maven para todos)
└── pom.xml                 # Dependencias Maven
```

---

## 🐳 Construir y correr en local

```bash
# Construir la imagen
docker build -t despacho:local .

# Correr (requiere una MySQL accesible)
docker run -p 8081:8081 \
  -e DB_ENDPOINT=<host-mysql> -e DB_PORT=3306 \
  -e DB_NAME=despacho_db -e DB_USERNAME=root -e DB_PASSWORD=<pass> \
  despacho:local
```

Endpoint principal: `GET http://localhost:8081/api/v1/despachos`

---

## ☁️ Despliegue en el clúster EKS

### Requisitos
- `aws` CLI con credenciales del AWS Academy Learner Lab
- `kubectl` configurado:
  ```bash
  aws eks update-kubeconfig --region us-east-1 --name innovatech-eks
  ```

### Desplegar
```bash
kubectl apply -f k8s/despacho.yaml
kubectl rollout status deployment/despacho -n innovatech
```

El microservicio queda como `despacho-service:8081` (ClusterIP), consumido por
el frontend vía DNS interno del clúster (CoreDNS).

---

## 🧩 Sobre el clúster (escalabilidad y disponibilidad)

EKS separa el clúster en dos planos (equivalente a manager/worker):

| Rol | En EKS | Función |
|-----|--------|---------|
| **Manager** | Plano de control (gestionado por AWS) | API server, scheduler, etcd |
| **Worker** | Node group (2× EC2 `t3.medium`) | Ejecutan los pods |

- **Alta disponibilidad:** 2 nodos en 2 zonas de disponibilidad distintas; si un
  nodo/pod cae, Kubernetes lo reprograma automáticamente (self-healing).
- **Escalar nodos (workers):**
  ```bash
  aws eks update-nodegroup-config --cluster-name innovatech-eks \
    --nodegroup-name innovatech-nodes \
    --scaling-config minSize=2,maxSize=4,desiredSize=3
  ```
- **Escalar pods (manual):**
  ```bash
  kubectl scale deployment/despacho --replicas=3 -n innovatech
  ```
- **Escalar pods (automático):** un **HPA** ajusta las réplicas según CPU (ver abajo).

---

## 📈 Autoscaling (HPA)

El `HorizontalPodAutoscaler` escala el microservicio entre 1 y 3 réplicas cuando
el CPU promedio supera el **50%**:

```bash
kubectl get hpa -n innovatech          # ver estado del autoscaling
kubectl top pods -n innovatech         # ver consumo de CPU/memoria
```

---

## ⚡ Capa serverless

Cola SQS + Lambdas (productor/consumidor) + API Gateway.
Ver instrucciones completas en **[`serverless/README.md`](serverless/README.md)**.

```bash
cd serverless && ./provision.sh    # provisiona todo (IaC)
```

---

## 💰 Bajar costos

```bash
cd serverless && ./teardown.sh     # borra la capa serverless
# Para borrar el clúster EKS: ver el repo del frontend / runbook
```
Demo CI/CD Sat Jun 27 09:53:44 -04 2026

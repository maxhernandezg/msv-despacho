# Microservicio Despacho — Despliegue Cloud-Native en AWS EKS

Microservicio **despacho** (Spring Boot) del sistema de gestión de despachos de
**Innovatech**, desplegado en un clúster **Kubernetes (AWS EKS)** con pipeline
**CI/CD** automatizado y una capa **serverless** (Lambda + SQS + API Gateway).

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
despacho/
├── Dockerfile              # Imagen del microservicio (multi-stage, Maven + JRE)
├── pom.xml                 # Dependencias Maven
├── src/                    # Código fuente Spring Boot
├── k8s/
│   └── despacho.yaml       # Deployment + Service + HPA
├── serverless/             # Capa serverless (Lambda + SQS + API Gateway)
│   ├── provision.sh        # IaC: provisiona todo (idempotente)
│   ├── teardown.sh         # Borra la capa serverless
│   ├── productor-despacho/
│   └── consumidor-despacho/
└── .github/workflows/
    └── deploy-eks.yml      # Pipeline build → push ECR → deploy EKS
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

## 🔁 CI/CD (GitHub Actions)

Cada `git push` a `main` dispara el pipeline `.github/workflows/deploy-eks.yml`:

1. **Build** de la imagen Docker
2. **Push** a Amazon ECR (tags `latest` + SHA del commit)
3. **Deploy** a EKS (`kubectl apply` + `set image` + `rollout status`)

**Secrets requeridos** (Settings → Secrets → Actions): `AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` (credenciales del Learner Lab).

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
Demo CI/CD Sat Jun 27 09:42:11 -04 2026
Demo CI/CD Sat Jun 27 09:53:44 -04 2026

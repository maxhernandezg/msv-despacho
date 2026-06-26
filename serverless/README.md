# Capa Serverless — Despacho (Lambda + SQS + API Gateway)

Implementación serverless (FaaS) que procesa despachos de forma **asíncrona**
mediante una cola, desacoplando la recepción del procesamiento.

## 🔗 Flujo

```
Cliente → API Gateway → Lambda productor → SQS → Lambda consumidor
 (HTTP POST)            (encola)          (cola)   (procesa)
```

1. El cliente hace `POST /despacho` al **API Gateway**.
2. La **Lambda productor** (`productor-despacho`) recibe el despacho y lo publica
   como mensaje en la **cola SQS** `despachos-queue`.
3. La **Lambda consumidor** (`consumidor-despacho`) se dispara automáticamente al
   llegar el mensaje y lo procesa.

**Ventaja:** el productor responde al instante (no espera al procesamiento); si
llegan muchos despachos de golpe, la cola los amortigua → resiliencia y escalabilidad.

## 📦 Componentes

| Componente | Nombre | Descripción |
|-----------|--------|-------------|
| Cola | `despachos-queue` | Cola SQS estándar (buzón asíncrono) |
| Lambda productor | `productor-despacho` | Recibe del API Gateway, encola en SQS |
| Lambda consumidor | `consumidor-despacho` | Trigger por SQS, procesa el mensaje |
| API Gateway | `despacho-api` | HTTP API, ruta `POST /despacho` |

Las Lambdas usan el rol **LabRole** (AWS Academy) como rol de ejecución.

## 🚀 Provisionar (Infraestructura como Código)

El script `provision.sh` crea **todo** de forma idempotente (re-ejecutable):

```bash
./provision.sh
```

Crea: cola SQS → Lambda productor (con env `QUEUE_URL`) → Lambda consumidor →
trigger SQS→consumidor → API Gateway con la ruta e integración → permisos.

Al final imprime la **URL de invocación**.

## 🧪 Probar el flujo completo

```bash
curl -X POST https://<API_ID>.execute-api.us-east-1.amazonaws.com/despacho \
  -H "Content-Type: application/json" \
  -d '{"idDespacho": 1, "cliente": "Juan Perez", "estado": "cerrado"}'
```

Respuesta esperada:
```json
{"ok": true, "mensaje": "Despacho encolado correctamente", "messageId": "..."}
```

Verificar el procesamiento en **CloudWatch → Log groups →
`/aws/lambda/consumidor-despacho`** (debe registrar "Despacho procesado correctamente").

## 🧹 Borrar (ahorro de créditos)

```bash
./teardown.sh
```

## 📁 Archivos

```
serverless/
├── provision.sh                       # IaC: provisiona toda la capa
├── teardown.sh                        # Borra toda la capa
├── productor-despacho/
│   └── lambda_function.py             # Productor (envía a SQS)
└── consumidor-despacho/
    └── lambda_function.py             # Consumidor (procesa de SQS)
```

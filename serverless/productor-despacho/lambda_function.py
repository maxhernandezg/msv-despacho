"""
Lambda PRODUCTOR - despacho
---------------------------
La invoca el API Gateway. Recibe los datos de un despacho y los
publica como mensaje en la cola SQS 'despachos-queue' (patrón productor).
El procesamiento real lo hace, de forma asíncrona, la Lambda consumidora.

Variables de entorno:
  QUEUE_URL = https://sqs.us-east-1.amazonaws.com/172591304240/despachos-queue

Rol de ejecución: LabRole (tiene permisos para SQS y CloudWatch Logs).
"""
import json
import os
import boto3

sqs = boto3.client("sqs")
QUEUE_URL = os.environ["QUEUE_URL"]


def lambda_handler(event, context):
    # El cuerpo de la petición HTTP viene en event["body"] (string JSON)
    body = event.get("body")
    if isinstance(body, str):
        try:
            data = json.loads(body)
        except json.JSONDecodeError:
            data = {"mensaje": body}
    else:
        data = body or {"mensaje": "despacho de prueba"}

    # Publica el mensaje en la cola (PRODUCTOR)
    respuesta = sqs.send_message(
        QueueUrl=QUEUE_URL,
        MessageBody=json.dumps(data),
    )

    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps({
            "ok": True,
            "mensaje": "Despacho encolado correctamente",
            "messageId": respuesta["MessageId"],
        }),
    }

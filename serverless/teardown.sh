#!/usr/bin/env bash
# ==============================================================================
# teardown.sh — Borra la capa serverless (limpieza / ahorro de créditos)
# Elimina: API Gateway, triggers, Lambdas y la cola SQS.
# Uso: ./teardown.sh
# ==============================================================================
set -uo pipefail

REGION="us-east-1"
API_NAME="despacho-api"
QUEUE_NAME="despachos-queue"

echo "==> Borrando API Gateway"
API_ID=$(aws apigatewayv2 get-apis --region "$REGION" \
  --query "Items[?Name=='$API_NAME'].ApiId | [0]" --output text 2>/dev/null)
if [ "$API_ID" != "None" ] && [ -n "$API_ID" ]; then
  aws apigatewayv2 delete-api --api-id "$API_ID" --region "$REGION" && echo "    API $API_ID borrada"
fi

echo "==> Borrando Lambdas y sus triggers"
for FN in productor-despacho consumidor-despacho; do
  for UUID in $(aws lambda list-event-source-mappings --function-name "$FN" --region "$REGION" \
                --query 'EventSourceMappings[].UUID' --output text 2>/dev/null); do
    aws lambda delete-event-source-mapping --uuid "$UUID" --region "$REGION" >/dev/null 2>&1 || true
  done
  aws lambda delete-function --function-name "$FN" --region "$REGION" 2>/dev/null \
    && echo "    $FN borrada" || true
done

echo "==> Borrando cola SQS"
QURL=$(aws sqs get-queue-url --queue-name "$QUEUE_NAME" --region "$REGION" \
  --query QueueUrl --output text 2>/dev/null || true)
if [ -n "${QURL:-}" ] && [ "$QURL" != "None" ]; then
  aws sqs delete-queue --queue-url "$QURL" --region "$REGION" && echo "    Cola borrada"
  echo "    (nota: SQS no deja recrear el mismo nombre por ~60s)"
fi

echo "✅ Limpieza serverless completa"

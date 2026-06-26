#!/usr/bin/env bash
# ==============================================================================
# provision.sh — Infraestructura como Código (IaC) de la capa serverless
# ------------------------------------------------------------------------------
# Provisiona de forma IDEMPOTENTE (re-ejecutable):
#   1. Cola SQS               (despachos-queue)
#   2. Lambda productor       (productor-despacho)  -> envía a la cola
#   3. Lambda consumidor      (consumidor-despacho) -> procesa la cola
#   4. Trigger SQS -> consumidor (event source mapping)
#   5. API Gateway HTTP       (despacho-api) POST /despacho -> Lambda productor
#
# Uso:        ./provision.sh
# Requisitos: aws CLI con credenciales del Learner Lab + rol LabRole.
# ==============================================================================
set -euo pipefail

# ---------------------------- Configuración -----------------------------------
REGION="us-east-1"
ACCOUNT_ID="172591304240"
LAB_ROLE="arn:aws:iam::${ACCOUNT_ID}:role/LabRole"
QUEUE_NAME="despachos-queue"
PRODUCTOR="productor-despacho"
CONSUMIDOR="consumidor-despacho"
API_NAME="despacho-api"
RUNTIME="python3.12"
HANDLER="lambda_function.lambda_handler"
ROUTE_KEY="POST /despacho"

cd "$(dirname "$0")"

# ---------------------------- 1. Cola SQS -------------------------------------
echo "==> 1. Cola SQS ($QUEUE_NAME)"
QUEUE_URL=$(aws sqs create-queue --queue-name "$QUEUE_NAME" --region "$REGION" \
  --query QueueUrl --output text 2>/dev/null || \
  aws sqs get-queue-url --queue-name "$QUEUE_NAME" --region "$REGION" \
  --query QueueUrl --output text)
QUEUE_ARN=$(aws sqs get-queue-attributes --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn --region "$REGION" --query 'Attributes.QueueArn' --output text)
echo "    $QUEUE_URL"

# ------------------- Función auxiliar: empaquetar y desplegar Lambda ----------
deploy_lambda () {
  local NAME="$1" DIR="$2"
  ( cd "$DIR" && zip -q -r "/tmp/${NAME}.zip" lambda_function.py )
  if aws lambda get-function --function-name "$NAME" --region "$REGION" >/dev/null 2>&1; then
    aws lambda update-function-code --function-name "$NAME" \
      --zip-file "fileb:///tmp/${NAME}.zip" --region "$REGION" >/dev/null
  else
    aws lambda create-function --function-name "$NAME" --runtime "$RUNTIME" \
      --role "$LAB_ROLE" --handler "$HANDLER" \
      --zip-file "fileb:///tmp/${NAME}.zip" --region "$REGION" >/dev/null
  fi
  # Esperar a que la función quede lista antes de actualizar su configuración
  aws lambda wait function-updated --function-name "$NAME" --region "$REGION"
}

# ---------------------------- 2. Lambda productor -----------------------------
echo "==> 2. Lambda productor"
deploy_lambda "$PRODUCTOR" "productor-despacho"
aws lambda update-function-configuration --function-name "$PRODUCTOR" \
  --environment "Variables={QUEUE_URL=$QUEUE_URL}" --region "$REGION" >/dev/null

# ---------------------------- 3. Lambda consumidor ----------------------------
echo "==> 3. Lambda consumidor"
deploy_lambda "$CONSUMIDOR" "consumidor-despacho"

# ---------------------------- 4. Trigger SQS -> consumidor --------------------
echo "==> 4. Trigger SQS -> consumidor"
if ! aws lambda list-event-source-mappings --function-name "$CONSUMIDOR" --region "$REGION" \
     --query "EventSourceMappings[?EventSourceArn=='$QUEUE_ARN'].UUID" --output text | grep -q .; then
  aws lambda create-event-source-mapping --function-name "$CONSUMIDOR" \
    --event-source-arn "$QUEUE_ARN" --batch-size 10 --region "$REGION" >/dev/null
  echo "    Trigger creado"
else
  echo "    Trigger ya existe (ok)"
fi

# ---------------------------- 5. API Gateway HTTP -----------------------------
echo "==> 5. API Gateway HTTP"
API_ID=$(aws apigatewayv2 get-apis --region "$REGION" \
  --query "Items[?Name=='$API_NAME'].ApiId | [0]" --output text)
if [ "$API_ID" = "None" ] || [ -z "$API_ID" ]; then
  API_ID=$(aws apigatewayv2 create-api --name "$API_NAME" --protocol-type HTTP \
    --region "$REGION" --query ApiId --output text)
  echo "    API creada: $API_ID"
else
  echo "    API ya existe: $API_ID"
fi

PRODUCTOR_ARN="arn:aws:lambda:${REGION}:${ACCOUNT_ID}:function:${PRODUCTOR}"

# Integración Lambda (reutiliza si ya apunta al productor)
INTEG_ID=$(aws apigatewayv2 get-integrations --api-id "$API_ID" --region "$REGION" \
  --query "Items[?IntegrationUri=='$PRODUCTOR_ARN'].IntegrationId | [0]" --output text)
if [ "$INTEG_ID" = "None" ] || [ -z "$INTEG_ID" ]; then
  INTEG_ID=$(aws apigatewayv2 create-integration --api-id "$API_ID" \
    --integration-type AWS_PROXY --integration-uri "$PRODUCTOR_ARN" \
    --payload-format-version 2.0 --region "$REGION" --query IntegrationId --output text)
fi

# Ruta POST /despacho
if ! aws apigatewayv2 get-routes --api-id "$API_ID" --region "$REGION" \
     --query "Items[?RouteKey=='$ROUTE_KEY'].RouteId" --output text | grep -q .; then
  aws apigatewayv2 create-route --api-id "$API_ID" --route-key "$ROUTE_KEY" \
    --target "integrations/$INTEG_ID" --region "$REGION" >/dev/null
fi

# Etapa por defecto con auto-deploy
aws apigatewayv2 create-stage --api-id "$API_ID" --stage-name '$default' \
  --auto-deploy --region "$REGION" >/dev/null 2>&1 || true

# Permiso para que API Gateway invoque la Lambda productora
aws lambda add-permission --function-name "$PRODUCTOR" --statement-id apigw-despacho \
  --action lambda:InvokeFunction --principal apigateway.amazonaws.com \
  --source-arn "arn:aws:execute-api:${REGION}:${ACCOUNT_ID}:${API_ID}/*/*/despacho" \
  --region "$REGION" >/dev/null 2>&1 || true

echo ""
echo "=============================================================="
echo "✅ Provisión serverless completa"
echo "   URL: https://${API_ID}.execute-api.${REGION}.amazonaws.com/despacho"
echo '   Probar:  curl -X POST <URL> -H "Content-Type: application/json" \'
echo '            -d {"idDespacho":1,"cliente":"Test","estado":"cerrado"}'
echo "=============================================================="

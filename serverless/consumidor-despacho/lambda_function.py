"""
Lambda CONSUMIDOR - despacho
----------------------------
Se dispara AUTOMÁTICAMENTE cuando llega un mensaje a la cola SQS
'despachos-queue' (patrón consumidor). Procesa cada despacho.

SQS entrega los mensajes en event["Records"]. Si la función termina
sin error, SQS borra los mensajes de la cola automáticamente.

Rol de ejecución: LabRole (permite a SQS invocar la Lambda y leer/borrar mensajes).
"""
import json


def lambda_handler(event, context):
    procesados = 0
    for record in event.get("Records", []):
        cuerpo = record["body"]
        try:
            despacho = json.loads(cuerpo)
        except json.JSONDecodeError:
            despacho = {"mensaje": cuerpo}

        # Aquí iría la lógica real: notificar, registrar, actualizar estado, etc.
        print(f"📦 Procesando despacho: {despacho}")
        print(f"✅ Despacho {despacho.get('idDespacho', '?')} "
              f"({despacho.get('cliente', 'sin cliente')}) procesado correctamente")
        procesados += 1

    print(f"Total de despachos procesados en este lote: {procesados}")
    return {"procesados": procesados}

# Guia de buenas practicas del repositorio

Documento de referencia del equipo para trabajar sobre el microservicio
**despacho**. Toda contribucion al repositorio debe respetar estas convenciones.

| | |
|---|---|
| Repositorio | `maxhernandezg/msv-despacho` |
| Modelo de ramificacion | Git Flow (adaptado) |
| Convencion de commits | Conventional Commits |
| Integracion | Pull Request obligatorio + GitHub Actions en verde |

---

## 1. Naming de ramas

El nombre de una rama debe permitir saber, sin abrir el codigo, **que tipo de
trabajo contiene y sobre que ramifica**.

| Prefijo | Nace de | Se integra a | Uso | Ejemplo |
|---------|---------|--------------|-----|---------|
| `main` | — | — | Codigo en produccion. Solo recibe merges de `develop` o de un `hotfix/`. | `main` |
| `develop` | `main` | `main` | Integracion continua del equipo. Contiene lo ya terminado y probado. | `develop` |
| `feature/` | `develop` | `develop` | Funcionalidad nueva o mejora. | `feature/pipeline-ci` |
| `hotfix/` | `main` | `main` **y** `develop` | Correccion urgente de un error en produccion. | `hotfix/location-header-post` |
| `release/` | `develop` | `main` y `develop` | Preparacion de una version (congelado de alcance). | `release/1.1.0` |

**Reglas de escritura**

- Minusculas, palabras separadas por guion medio: `feature/validacion-fecha`.
- Sin tildes, sin enes, sin espacios ni caracteres especiales.
- Descriptivo y corto (maximo 4 palabras): dice **que** se hace, no quien lo hace.
- Una rama = un proposito. Si aparece trabajo no relacionado, se abre otra rama.
- No se reutiliza una rama ya integrada: se crea una nueva desde `develop`.

## 2. Mensajes de commit

Se usa **Conventional Commits**: `<tipo>(<alcance opcional>): <descripcion>`

| Tipo | Cuando se usa |
|------|---------------|
| `feat` | Nueva funcionalidad visible para el usuario o consumidor de la API |
| `fix` | Correccion de un error |
| `docs` | Solo documentacion |
| `test` | Agregar o corregir pruebas |
| `ci` | Cambios en workflows de GitHub Actions |
| `build` | Cambios en el empaquetado, Dockerfile, Maven o dependencias |
| `refactor` | Cambio interno que no altera el comportamiento |
| `chore` | Mantenimiento (configuracion, .gitignore, limpieza) |

**Reglas**

- Descripcion en **imperativo** y en minuscula: `agregar`, no `agregado` ni `Agrega`.
- Maximo 72 caracteres en la primera linea, sin punto final.
- Cuerpo opcional separado por una linea en blanco, explicando **por que** se
  hizo el cambio (el *que* ya se ve en el diff).
- Un commit = un cambio logico. Nada de `cambios varios` ni `arreglos`.

```
feat(api): validar que la fecha de despacho no sea anterior a hoy

El endpoint aceptaba fechas pasadas y generaba registros inconsistentes
con el microservicio de venta.
```

Ejemplos incorrectos: `comentario`, `cambios`, `update`, `asdasd`, `arreglo final ahora si`.

## 3. Estructura de carpetas

```
msv-despacho/
├── .github/
│   ├── workflows/            # Automatizacion CI/CD (GitHub Actions)
│   │   ├── ci.yml            # CI  : build + pruebas (push a develop, PR a main)
│   │   └── deploy-eks.yml    # CD  : imagen a ECR y despliegue a EKS (push a main)
│   ├── ISSUE_TEMPLATE/       # Plantillas de reporte de errores y solicitudes
│   └── PULL_REQUEST_TEMPLATE.md
├── docs/                     # Documentacion del proyecto
│   └── bitacora-git.md       # Trazabilidad del flujo colaborativo
├── k8s/                      # Manifiestos de Kubernetes
├── src/
│   ├── main/java/com/citt/   # Codigo fuente (controller, services, persistence)
│   ├── main/resources/       # Configuracion de la aplicacion
│   └── test/                 # Pruebas automatizadas y su configuracion (H2)
├── .gitignore
├── CONTRIBUTING.md           # Esta guia
├── Dockerfile
├── README.md
├── mvnw / mvnw.cmd / .mvn/   # Maven Wrapper (misma version de Maven para todos)
└── pom.xml
```

**Que nunca se versiona** (ver `.gitignore`): `target/` y cualquier artefacto
compilado, `.idea/` y configuracion del IDE, `.DS_Store`, archivos `.env`,
llaves `.pem` y credenciales. Los secretos viven en
*Settings → Secrets and variables → Actions*, nunca en el repositorio.

## 4. Control de versiones

- **Versionado semantico** (`MAYOR.MENOR.PARCHE`) sobre `main`, con etiquetas:

  ```bash
  git tag -a v1.1.0 -m "Pipeline de CI e integracion continua"
  git push origin v1.1.0
  ```

  - `MAYOR`: cambio incompatible en la API del microservicio.
  - `MENOR`: funcionalidad nueva compatible hacia atras.
  - `PARCHE`: correccion de errores (todo `hotfix/`).
- Cada imagen publicada en ECR se etiqueta con el **SHA del commit**, de modo
  que cualquier contenedor en ejecucion es rastreable hasta la linea de codigo
  que lo origino.
- El historial de `main` no se reescribe: **prohibido** `push --force` sobre
  `main` y `develop`. Un error en produccion se corrige con un nuevo commit
  (`hotfix/`) o con `git revert`, nunca borrando historia.

## 5. Flujo de merge

1. La rama de trabajo se crea siempre desde su rama base actualizada
   (`git pull` antes de ramificar).
2. Antes de abrir el PR se sincroniza con la base para resolver conflictos
   en local: `git merge develop` (o `git pull origin develop`).
3. La integracion se hace **exclusivamente por Pull Request**. No se hace
   `push` directo a `main` ni a `develop`.
4. Se usa **merge commit** (`--no-ff`) y no *squash*: el historial conserva los
   commits individuales, requisito de trazabilidad del proyecto.
5. Un PR solo se integra si el workflow de CI esta en verde y tiene la
   aprobacion de la contraparte del equipo.

## 6. Estrategia de revision de codigo

- **Revision cruzada obligatoria**: quien escribe el codigo no aprueba su
  propio PR; lo revisa el otro integrante del equipo.
- El PR debe usar la plantilla del repositorio (objetivo, cambios, como
  verificar y checklist).
- PRs pequenos: idealmente menos de 400 lineas modificadas. Si crece mas, se
  divide en varios.
- Que revisa quien aprueba:
  1. El CI esta en verde (compila y las pruebas pasan).
  2. El cambio hace lo que dice el titulo y nada mas.
  3. Nombres de rama y mensajes de commit siguen la convencion.
  4. No se filtran credenciales ni archivos generados.
  5. Hay pruebas o evidencia de verificacion del cambio.
- Los comentarios de revision se redactan sobre el codigo, no sobre la persona,
  y se marcan como *bloqueantes* o *sugerencias*.
- Las conversaciones abiertas deben resolverse antes de integrar.

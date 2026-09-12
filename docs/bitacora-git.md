# Bitacora de trabajo colaborativo con Git

Registro de la simulacion del flujo colaborativo sobre el microservicio
**despacho**, con los comandos ejecutados en cada etapa y los Pull Requests
integrados. Sirve como evidencia de trazabilidad del codigo fuente.

Repositorio: <https://github.com/maxhernandezg/msv-despacho>

---

## 1. Preparacion del repositorio

```bash
# Clonar el repositorio remoto
git clone https://github.com/maxhernandezg/msv-despacho.git
cd msv-despacho

# Crear la rama de integracion a partir de main y publicarla
git checkout -b develop main
git push -u origin develop
```

## 2. Feature 1 — Pipeline de integracion continua

```bash
# Rama de trabajo a partir de develop
git checkout develop
git pull
git checkout -b feature/pipeline-ci

# ... cambios: ci.yml, .gitignore, H2 para pruebas, maven wrapper ...

# Sacar del control de versiones los artefactos generados
git rm -r --cached target
git rm --cached .DS_Store

# Commits atomicos siguiendo Conventional Commits
git add .gitignore
git commit -m "chore: ignorar artefactos de compilacion y archivos de entorno local"
git add .mvn
git commit -m "build: restaurar maven-wrapper.properties del wrapper"
git add src/test/resources pom.xml
git commit -m "test: configurar base de datos H2 en memoria para las pruebas"
git add .github/workflows
git commit -m "ci: agregar workflow de integracion continua con GitHub Actions"

# Publicar la rama y abrir el Pull Request hacia develop
git push -u origin feature/pipeline-ci
gh pr create --base develop --head feature/pipeline-ci
```

Resultado: el workflow **CI** se ejecuta automaticamente sobre el Pull Request.
Con los dos jobs en verde (pruebas e imagen Docker) el cambio se integra:

```bash
gh pr merge 1 --merge          # merge commit (--no-ff), conserva el historial
```

## 3. Feature 2 — Convenciones y guia de buenas practicas

```bash
git checkout develop
git pull                        # traer el feature 1 ya integrado
git checkout -b feature/documentacion-git

# ... cambios: README.md, CONTRIBUTING.md, plantillas de PR e issues,
#     CODEOWNERS y esta bitacora ...

git add .
git commit -m "docs: documentar estrategia de ramificacion y convenciones del equipo"
git push -u origin feature/documentacion-git
gh pr create --base develop --head feature/documentacion-git
gh pr merge 2 --merge
```

## 4. Publicacion a produccion (release)

```bash
# Integrar a main todo lo acumulado y validado en develop
gh pr create --base main --head develop --title "release: pipeline de CI y documentacion"
gh pr merge 3 --merge
```

El Pull Request hacia `main` vuelve a ejecutar el workflow de CI (evento
`pull_request`), y al integrarse dispara el workflow de CD hacia EKS.

## 5. Hotfix — correccion urgente sobre produccion

```bash
# La rama de correccion nace de main, no de develop
git checkout main
git pull
git checkout -b hotfix/location-header-post

# ... correccion del encabezado Location del endpoint POST ...

git commit -am "fix(api): construir el encabezado Location despues de persistir"
git push -u origin hotfix/location-header-post
gh pr create --base main --head hotfix/location-header-post
gh pr merge 4 --merge

# Git Flow: el hotfix debe volver tambien a develop para no perder la correccion
gh pr create --base develop --head main --title "chore: sincronizar develop con el hotfix"
gh pr merge 5 --merge
```

## 6. Creditos del equipo

```bash
git checkout develop
git pull
git checkout -b feature/equipo-y-revisores

# ... usuarios de GitHub en el README y segundo integrante en CODEOWNERS ...

# Commit con autoria compartida (aparece a nombre de ambos en GitHub)
git commit -m "docs: identificar a ambos integrantes como autores y revisores

Co-Authored-By: Marcos del Canto <133884986+MarcosdelCanto@users.noreply.github.com>"
git push -u origin feature/equipo-y-revisores
gh pr create --base develop --head feature/equipo-y-revisores
gh pr merge 6 --merge

# Publicacion a main
gh pr create --base main --head develop --title "release: creditos del equipo"
gh pr merge 7 --merge
```

## 7. Comandos de verificacion y trazabilidad

```bash
git log --oneline --graph --all      # historial ramificado completo
git log --merges --oneline           # puntos de integracion (merge de cada PR)
git branch -a                        # ramas locales y remotas
git show <sha>                       # detalle de un commit puntual
git diff main develop                # diferencias entre produccion e integracion
gh pr list --state all               # todos los Pull Requests del repositorio
gh run list                          # ejecuciones de los workflows
```

---

## Resumen de Pull Requests

| # | Rama origen | Destino | Tipo | Descripcion |
|---|-------------|---------|------|-------------|
| 1 | `feature/pipeline-ci` | `develop` | feature | Workflow de CI, pruebas ejecutables en runner y limpieza del repositorio |
| 2 | `feature/documentacion-git` | `develop` | feature | Estrategia de ramificacion, convenciones y plantillas de colaboracion |
| 3 | `develop` | `main` | release | Publicacion a produccion de lo integrado en develop |
| 4 | `hotfix/location-header-post` | `main` | hotfix | Correccion del encabezado `Location` en el POST |
| 5 | `main` | `develop` | chore | Sincronizacion de develop con la correccion publicada |
| 6 | `feature/equipo-y-revisores` | `develop` | feature | Creditos del equipo y revisores (CODEOWNERS) |
| 7 | `develop` | `main` | release | Publicacion de los creditos del equipo |
| 8 | `feature/bitacora-final` | `develop` | docs | Cierre de la bitacora de trazabilidad |
| 9 | `develop` | `main` | release | Publicacion del estado final del repositorio |

Todos los Pull Requests se integraron con **merge commit** (`--no-ff`) y con el
workflow de CI en verde. Las ramas no se eliminaron tras el merge, de modo que
`main`, `develop`, `feature/*` y `hotfix/*` permanecen visibles en el
repositorio como evidencia del modelo de ramificacion utilizado.

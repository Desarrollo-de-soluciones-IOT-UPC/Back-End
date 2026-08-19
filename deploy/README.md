# Despliegue de EMSafe en producción

Infraestructura auto-hospedada sobre una VM ARM del tier **Always Free** de
Oracle Cloud. Costo de operación: **0 USD/mes**.

- **Aplicación:** https://emsafe.duckdns.org
- **API / Swagger:** https://emsafe.duckdns.org/swagger-ui/index.html

---

## 1. Arquitectura

```
                    Internet
                       │
                       ▼  :80 / :443
        ┌──────────────────────────────┐
        │  Caddy  (contenedor "web")   │  TLS automático (Let's Encrypt)
        │  + SPA Angular en /srv       │
        └───────┬──────────────┬───────┘
                │              │
      /api/*    │              │  /  (todo lo demás)
      /ws       │              └──▶ archivos estáticos + fallback SPA
                ▼
        ┌───────────────────┐
        │ Spring Boot :8080 │  (contenedor "backend")
        └─────────┬─────────┘
                  ▼
        ┌───────────────────┐
        │   MySQL 8 :3306   │  (contenedor "db", red interna, sin puertos)
        └───────────────────┘
```

Un **único dominio** para todo. Esto elimina de raíz tres problemas que existían
en el despliegue anterior: CORS entre dominios distintos, contenido mixto
(http dentro de https) y la configuración especial que necesitaba el WebSocket.

Ni el backend ni MySQL publican puertos al exterior: solo Caddy es alcanzable
desde internet.

---

## 2. Requisitos

| | |
|---|---|
| VM | Oracle Cloud `VM.Standard.A1.Flex` (ARM), Ubuntu 24.04, Always Free |
| Puertos | 80 y 443 abiertos en la **Security List** de OCI **y** en el iptables de la VM |
| Dominio | Subdominio de DuckDNS apuntando a la IP pública |
| Llave | Par SSH ed25519 registrado al crear la instancia |

---

## 3. Despliegue desde cero

### 3.1 Conectar

```bash
ssh -i ~/.ssh/id_ed25519 ubuntu@<IP_PUBLICA>
```

### 3.2 Preparar el sistema

```bash
sudo apt-get update && sudo apt-get -y upgrade
sudo apt-get -y install ca-certificates curl git nano
```

### 3.3 Swap (recomendado)

Compilar Angular es lo más pesado de todo el proceso. Con 2 GB de swap el build
no muere por falta de memoria aunque la VM sea de 6 GB.

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 3.4 Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
docker --version
```

### 3.5 Abrir los puertos en la VM

**Este es el paso que más se olvida.** Las imágenes Ubuntu de Oracle traen
iptables cerrado por defecto, así que abrir los puertos en la Security List de
la consola **no es suficiente**: hay que abrirlos también aquí dentro.

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80  -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

### 3.6 Clonar los repositorios

Deben quedar como **hermanos**: el compose construye el frontend desde
`../../Front-End`.

```bash
mkdir -p ~/emsafe && cd ~/emsafe
git clone https://github.com/Desarrollo-de-soluciones-IOT-UPC/Back-End.git
git clone https://github.com/Desarrollo-de-soluciones-IOT-UPC/Front-End.git

# La rama por defecto de Front-End es 'develop', pero el código vigente
# está en 'main'.
cd Front-End && git checkout main && cd ..
```

### 3.7 Secretos

```bash
cd ~/emsafe/Back-End/deploy
cp .env.example .env

openssl rand -base64 24   # -> MYSQL_ROOT_PASSWORD
openssl rand -base64 24   # -> MYSQL_PASSWORD
openssl rand -base64 48   # -> JWT_SECRET

nano .env
```

El `.env` está en el `.gitignore`. **Nunca debe llegar al repositorio.**

### 3.8 Apuntar el dominio

```bash
curl "https://www.duckdns.org/update?domains=emsafe&token=TU_TOKEN&ip="
dig +short emsafe.duckdns.org      # debe devolver la IP de la VM
```

Espera a que resuelva **antes** de levantar el stack: Let's Encrypt valida el
dominio por HTTP y fallará si el DNS todavía no propagó.

### 3.9 Levantar

```bash
cd ~/emsafe/Back-End/deploy
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f
```

El primer arranque tarda: hay que compilar el backend con Maven y la SPA con
Angular sobre ARM. Entre 5 y 12 minutos según el tamaño de la instancia.

Está listo cuando en los logs de `web` aparece el certificado emitido y
`backend` reporta `Started EmsafeBackendApplication`.

---

## 4. Operación

```bash
cd ~/emsafe/Back-End/deploy

# Estado y logs
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend

# Desplegar cambios
cd ~/emsafe/Back-End && git pull
cd ~/emsafe/Front-End && git pull
cd ~/emsafe/Back-End/deploy
docker compose -f docker-compose.prod.yml up -d --build

# Reiniciar un solo servicio
docker compose -f docker-compose.prod.yml restart backend
```

### Respaldos

```bash
chmod +x backup.sh
./backup.sh                                    # manual

# diario a las 04:15
(crontab -l 2>/dev/null; echo "15 4 * * * $(pwd)/backup.sh >> $(pwd)/backups/backup.log 2>&1") | crontab -
```

Restaurar:

```bash
gunzip -c backups/emsafe_db_FECHA.sql.gz | \
  docker compose -f docker-compose.prod.yml exec -T db \
  mysql -u root -p"$MYSQL_ROOT_PASSWORD" emsafe_db
```

### Mantener el DNS al día

La IP pública de OCI es estable mientras la instancia exista, pero si alguna vez
se recrea, este cron reapunta el dominio solo:

```bash
(crontab -l 2>/dev/null; echo "*/15 * * * * curl -fsS 'https://www.duckdns.org/update?domains=emsafe&token=TU_TOKEN&ip=' >/dev/null") | crontab -
```

---

## 5. Problemas frecuentes

| Síntoma | Causa habitual |
|---|---|
| Caddy no consigue el certificado | 80/443 cerrados en la Security List **o** en el iptables de la VM; o el DNS aún no resuelve |
| `backend` reinicia en bucle | credenciales de MySQL mal en `.env`, o Flyway falló. Revisa `logs backend` |
| El build de Angular muere sin mensaje | falta memoria: crea el swap del punto 3.3 |
| 502 al entrar | el backend todavía está arrancando (Flyway migra al inicio). Espera y revisa el healthcheck |
| El chat "Astra" no responde | `GEMINI_API_KEY` vacía o revocada |
| La app carga pero las rutas dan 404 | falta el `try_files` del Caddyfile |

Nunca borres el volumen `caddy_data`: contiene los certificados. Recrearlos
demasiadas veces choca con los límites de emisión de Let's Encrypt.

---

## 6. Decisiones de arquitectura

### ADR-001 — De Azure App Service a Oracle Cloud auto-hospedado

**Estado:** aceptado · agosto 2026

**Contexto.** El sistema estaba desplegado en Azure App Service (backend y
frontend) con créditos de estudiante. Al agotarse los créditos el despliegue
quedó fuera de línea. El proyecto debe seguir accesible de forma permanente como
pieza de portafolio, sin costo recurrente.

**Opciones evaluadas.**

1. *Renovar los créditos de Azure para estudiantes.* Cero cambios técnicos, pero
   el problema reaparece cada 12 meses y depende de seguir matriculado.
2. *PaaS con capa gratuita (Render, Koyeb).* Despliegue sencillo, pero los planes
   gratuitos suspenden el servicio tras 15-60 min sin tráfico, con arranques en
   frío de ~50 s. Además, las 750 h/mes de Render son por *workspace*: un segundo
   servicio gratuito habría comprometido otro proyecto ya desplegado ahí.
3. *VM Always Free de Oracle Cloud.* 2 OCPU ARM / 12 GB / 200 GB sin caducidad y
   sin suspensión por inactividad. Exige administrar el servidor.

**Decisión.** Opción 3. Es la única que ofrece disponibilidad continua real a
costo cero, y la capacidad sobra para ejecutar los tres servicios en una sola
máquina.

**Consecuencias.**

- ✅ Sin arranques en frío ni suspensión; comportamiento de producción real.
- ✅ Recursos muy holgados para el tamaño del sistema.
- ✅ Un solo dominio elimina el CORS y la configuración especial del WebSocket.
- ⚠️ Los respaldos, las actualizaciones del SO y el monitoreo pasan a ser
  responsabilidad nuestra (mitigado con `backup.sh` y los healthchecks).
- ⚠️ Un único nodo: no hay alta disponibilidad. Aceptable para este alcance.
- ⚠️ Oracle redujo el tier gratuito de 4 OCPU/24 GB a 2 OCPU/12 GB en julio 2026.
  Podría volver a recortarlo.

### ADR-002 — De Angular SSR a SPA estática

**Estado:** aceptado · agosto 2026

**Contexto.** El frontend se compilaba con `outputMode: "server"`, lo que exigía
un proceso Node activo en producción para renderizar en servidor.

**Decisión.** Compilarlo como SPA estática y servirlo directamente desde Caddy.

**Razones.**

- El panel está **detrás de autenticación**: ningún buscador puede indexarlo, así
  que el SEO —principal motivo para usar SSR— no aplica.
- El contenido público que sí necesita indexarse es la *landing page*, que ya es
  HTML estático independiente.
- Elimina un proceso permanente de ~200 MB de RAM y una pieza menos que puede
  fallar.
- El despliegue se reduce a copiar archivos: sin runtime de Node en producción.

**Consecuencias.**

- ✅ Menos memoria, menos superficie de fallo, arranque instantáneo.
- ✅ Los archivos estáticos se cachean de forma trivial.
- ⚠️ La primera carga renderiza en el cliente. Irrelevante en un panel interno.
- ⚠️ Se desactivó el *inlining* de fuentes (`optimization.fonts.inline: false`)
  para que la compilación no dependa de alcanzar Google Fonts en tiempo de build.
  Las fuentes se cargan desde el CDN en tiempo de ejecución, como ya hacía
  `index.html`.
- 🔄 Reversible: basta restaurar `server`, `ssr` y `outputMode` en `angular.json`.

**Archivos que quedaron sin uso** (conservados por si se revierte): `src/server.ts`,
`src/main.server.ts`, `src/app/app.config.server.ts`, `src/app/app.routes.server.ts`.
Están excluidos en `tsconfig.app.json`, así que no entran en la compilación.

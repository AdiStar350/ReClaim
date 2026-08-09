Here’s a practical VPS deployment guide for **`AdiStar350/ReClaim`**.

This guide assumes:

- backend runs on the VPS
- MongoDB stays on **MongoDB Atlas**
- Android app connects to the VPS over the internet
- you use **Docker**
- optional but recommended: a domain + HTTPS

---

# 1. What you are deploying

From this repo, the backend is in:

- `reclaim-backend/`

It is:

- **Spring Boot**
- **Java 17**
- packaged with **Maven**
- runnable with **Docker**

The repo already contains:

- `reclaim-backend/Dockerfile`
- `reclaim-backend/pom.xml`

The backend listens on:

- `PORT`, default `8080`

The backend needs:

- `JWT_SECRET`
- MongoDB config via either:
  - `MONGODB_URI`, or
  - `MONGODB_USER`, `MONGODB_PASSWORD`, `MONGODB_HOST`, `MONGODB_DATABASE`

I confirmed that from:

- `reclaim-backend/src/main/resources/application.properties`
- `reclaim-backend/src/main/java/com/example/reclaimbackend/config/MongoConfig.java`

---

# 2. Get a VPS

Any Linux VPS is fine. Ubuntu 22.04 or 24.04 is easiest.

Minimum suggested:

- 1 vCPU
- 1–2 GB RAM
- 20 GB storage

After buying/creating the VPS, you’ll have:

- a public IP
- an SSH login like `root` or another user

SSH in:

```bash
ssh root@YOUR_VPS_IP
```

---

# 3. Install Docker on the VPS

On Ubuntu:

```bash
apt update
apt install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker
```

Test:

```bash
docker --version
```

---

# 4. Clone your repo on the VPS

Install git if needed:

```bash
apt install -y git
```

Clone:

```bash
git clone https://github.com/AdiStar350/ReClaim.git
cd ReClaim
```

---

# 5. Configure environment variables

Your backend supports env vars. Since you’re using Docker, the cleanest approach is to create a file for runtime variables.

In the repo root, create `.env`:

```bash
nano .env
```

Example contents:

```dotenv
MONGODB_URI=mongodb+srv://YOUR_USER:YOUR_PASSWORD@YOUR_CLUSTER.mongodb.net/reclaim?retryWrites=true&w=majority
MONGODB_DATABASE=reclaim
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
JWT_EXPIRATION_MS=86400000
PORT=8080
```

You can also use the split values instead of `MONGODB_URI`:

```dotenv
MONGODB_USER=your_atlas_db_user
MONGODB_PASSWORD=your_atlas_db_password
MONGODB_HOST=your-cluster.mongodb.net
MONGODB_DATABASE=reclaim
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
JWT_EXPIRATION_MS=86400000
PORT=8080
```

Because `MongoConfig.java` checks:

- split values first, then
- `MONGODB_URI`

So either style works.

Generate a good JWT secret, for example:

```bash
openssl rand -base64 48
```

---

# 6. Allow MongoDB Atlas access from the VPS

In MongoDB Atlas:

- go to **Network Access**
- add your VPS public IP

Without this, the backend may start but fail to connect to MongoDB.

---

# 7. Build the backend Docker image

Go into the backend folder:

```bash
cd ~/ReClaim/reclaim-backend
```

Build:

```bash
docker build -t reclaim-backend .
```

This uses the repo’s Dockerfile, which:

- builds the app with Maven in a JDK 17 image
- copies the final jar into a lighter JRE 17 image
- exposes port 8080

---

# 8. Run the backend container

Because your `.env` file is in the repo root and not inside `reclaim-backend`, the simplest choice is to pass env vars directly with `--env-file`.

From `~/ReClaim/reclaim-backend` run:

```bash
docker run -d \
  --name reclaim-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file ../.env \
  reclaim-backend
```

Check logs:

```bash
docker logs -f reclaim-backend
```

Check running containers:

```bash
docker ps
```

---

# 9. Test the backend from the VPS

Test locally on the server:

```bash
curl http://localhost:8080
```

If the root route is not defined, that may return 404, which is okay. The important part is that the server responds.

You can also inspect logs:

```bash
docker logs reclaim-backend
```

If there is a DB problem, you’ll likely see Mongo connection errors.

---

# 10. Open firewall ports

If your VPS provider or Ubuntu firewall is enabled, allow traffic.

For Ubuntu UFW:

```bash
ufw allow OpenSSH
ufw allow 8080
ufw enable
ufw status
```

Now test from your own computer:

```bash
http://YOUR_VPS_IP:8080
```

For quick testing, this is enough.

---

# 11. Better setup: use a domain and HTTPS

For production, do **not** leave it as raw `http://IP:8080`.

Better:

- domain or subdomain, e.g. `api.yourdomain.com`
- reverse proxy
- HTTPS

## Domain setup

At your domain registrar:

- create an **A record**
- point `api.yourdomain.com` to `YOUR_VPS_IP`

Wait for DNS to propagate.

---

# 12. Install Nginx

```bash
apt install -y nginx
systemctl enable nginx
systemctl start nginx
```

Create config:

```bash
nano /etc/nginx/sites-available/reclaim
```

Paste:

```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable it:

```bash
ln -s /etc/nginx/sites-available/reclaim /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

Now:

```text
http://api.yourdomain.com
```

should forward to your backend.

---

# 13. Add free HTTPS with Let’s Encrypt

Install certbot:

```bash
apt install -y certbot python3-certbot-nginx
```

Run:

```bash
certbot --nginx -d api.yourdomain.com
```

Choose redirect to HTTPS when prompted.

Now your backend should be available at:

```text
https://api.yourdomain.com
```

That is what your Android app should use.

---

# 14. Update the Android app

Your app currently uses:

```java
private static final String BASE_URL = "http://10.0.2.2:8080/";
```

That only works for the emulator on your development machine.

You need to change it in:

- `reclaim-project/app/src/main/java/com/example/reclaim/network/RetrofitClient.java`

Replace with:

```java
private static final String BASE_URL = "https://api.yourdomain.com/";
```

Or for temporary direct-IP testing:

```java
private static final String BASE_URL = "http://YOUR_VPS_IP:8080/";
```

Then rebuild and reinstall the app on your devices.

---

# 15. How multiple Android devices work

Nothing special is required beyond using a public backend URL.

All devices will call the same backend:

- phone 1 → VPS backend
- phone 2 → VPS backend
- phone 3 → VPS backend

The backend is a normal shared API server. MongoDB Atlas stores the shared data.

So once the URL is public and reachable, multiple devices can use it at the same time.

---

# 16. Useful Docker commands

Stop container:

```bash
docker stop reclaim-backend
```

Start container:

```bash
docker start reclaim-backend
```

Restart:

```bash
docker restart reclaim-backend
```

View logs:

```bash
docker logs -f reclaim-backend
```

Remove container:

```bash
docker rm -f reclaim-backend
```

Rebuild after code changes:

```bash
cd ~/ReClaim
git pull
cd reclaim-backend
docker rm -f reclaim-backend
docker build -t reclaim-backend .
docker run -d \
  --name reclaim-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file ../.env \
  reclaim-backend
```

---

# 17. Recommended production checklist

Before using real devices, verify:

- Docker container is running
- MongoDB Atlas allows VPS IP
- `.env` contains valid values
- JWT secret is strong
- port 8080 works internally
- Nginx proxies to 8080
- HTTPS works
- Android app uses the public HTTPS URL
- backend logs show successful startup

---

# 18. If you want the simplest possible first test

Do this first:

1. Deploy on VPS with Docker
2. Open port 8080
3. Change Android app base URL to:

```java
http://YOUR_VPS_IP:8080/
```

1. Test from one device
2. After it works, add domain + HTTPS

That’s the fastest path.

---

# 19. Best architecture for this repo

For **ReClaim**, the clean setup is:

- **Android app** on devices
- **Spring Boot backend** on VPS
- **MongoDB Atlas** as DB
- **Firebase Storage** for image storage
- **Nginx + Let’s Encrypt** in front of the backend

That avoids Render sleeping and stays relatively simple.

---

# 20. One important repo note

The code search results I used are limited and may be incomplete. I verified the key config files relevant to deployment, but if you want a full audit of every environment-dependent setting, I can inspect more of the repo.

If you want, I can next give you either:

1. a **copy-paste `docker-compose.yml` + Nginx config** for this repo, or
2. a **guide for changing the Android app to use different URLs for emulator vs production**.

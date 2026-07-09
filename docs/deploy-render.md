# Render Docker Deployment

RoomFit Backend can be deployed to Render as a Free Web Service with Docker.

## Render Settings

1. Create a new Render Web Service from the GitHub repository.
2. Select `Docker` as the Runtime/Language.
3. Select the `main` branch.
4. Select `Free` as the Instance Type.
5. Set the Health Check Path to `/api/products/mock`.

Render provides the `PORT` environment variable at runtime. The Dockerfile starts Spring Boot with `-Dserver.port=${PORT:-10000}` and binds to `0.0.0.0`, so it uses Render's port when present and falls back to `10000` locally.

## Check After Deploy

Replace `<render-service-name>` with the Render service name.

```text
https://<render-service-name>.onrender.com/api/products/mock
https://<render-service-name>.onrender.com/swagger-ui/index.html
```

## Notes

- Render Free instances can sleep when idle, so the first request after inactivity can be slow.
- This MVP uses in-memory repositories. Uploaded room/context/layout data can be reset when the service restarts or redeploys.
- Local Docker build command:

```bash
docker build -t roomfit-backend .
```

Optional local run command:

```bash
docker run --rm -p 10000:10000 -e PORT=10000 roomfit-backend
```

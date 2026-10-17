# CMR Sign

Open-source Kotlin library and demo app for **personal electronic signatures** on freight documents such as CMR consignment notes and eFTI records.

## What works now

- Create a signing request from a PDF and fetch its state.
- Protect platform routes with a shared API key.
- Start a registered-signer demo session, prove identity, and store the signer.
- Sign through a provider interface and produce PAdES signatures.
- Validate with EU DSS:
  - valid → `TOTAL_PASSED`
  - tampered → `TOTAL_FAILED` / `HASH_FAILURE`
  - untrusted chain → `INDETERMINATE` / `NO_CERTIFICATE_CHAIN_FOUND`
  - expired certificate → `INDETERMINATE` / `SIG_CONSTRAINTS_FAILURE`
- Store the signed document as an immutable new version.
- Download document versions through the API.
- Try the demo UI: create → start session → sign → see verdict.

## Not implemented yet

- Real e-ID/QTSP provider and AdES/QES qualification.
- Signer consent with a visible document hash.
- Persisted validation reports and webhooks.
- Ad hoc signing links and signer pages.
- Long-term validity (B-LT / B-LTA).
- Revoked-certificate validation.
- OpenAPI description.
- Downloading the signed document from the UI.

## Reference provider warning

The demo uses a **self-signed reference identity**, not a real trust service.

- The derived qualification is `NA`, never AdES or QES.
- The signature is **not a legally valid eIDAS signature**.
- Provider sessions are in memory; restarting the demo between “Start session” and “Sign” loses the session.

The code is built around a `SigningProvider` interface. Wiring a real e-ID/QTSP adapter is the next step, so an integrator can bring their own provider.

## Using the libraries

Add `core` to your JVM application and implement `SigningProvider`. Add `validation-dss` only if you need in-process DSS validation. `core` has no web, database or framework dependency.

## Project layout

| Module | Purpose |
|---|---|
| `core` | Framework-free domain, state machine, value types, signing and provider interfaces |
| `validation-dss` | EU DSS-backed validator; the only module that imports DSS |
| `demo` | Optional HTTP demo app, PostgreSQL storage and UI |

## Getting started

Prerequisites: Docker with Compose for the demo; JDK 25 for local Gradle builds.

### Run the demo

```bash
cp .env.example .env
# set PLATFORM_API_KEY=dev-insecure-key for the bundled UI
docker compose up --build
```

Open <http://localhost:8080>.

### Frontend development

```bash
docker compose up -d db
cp .env demo/.env
./gradlew :demo:run

cd demo/ui
npm install
npm run dev
```

Open <http://localhost:8000>. Vite proxies `/api` and `/health` to `localhost:8080`.

### Demo flow

1. Upload `core/src/mocks/resources/example-unsigned.pdf`.
2. **Create request** → `CREATED`.
3. **Start session** → `IDENTITY_PROVEN`.
4. **Sign** → `VALIDATED` with the verdict shown.
5. Download version 2 through the API:

```bash
curl -H "X-Api-Key: dev-insecure-key" \
  -o signed.pdf \
  http://localhost:8080/api/envelopes/{id}/documents/2
```

## API

Platform routes require `X-Api-Key`. The health check is public.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/envelopes` | Create a request; JSON body with a base64 `payload` |
| `GET` | `/api/envelopes/{id}` | Read the request state |
| `POST` | `/api/envelopes/{id}/start` | Start the reference signer session and prove identity |
| `POST` | `/api/envelopes/{id}/sign` | Sign the document, validate it and return the verdict |
| `GET` | `/api/envelopes/{id}/documents/{version}` | Download a document version; `ETag` is the SHA-256 |
| `GET` | `/health` | Liveness |

## Configuration

| Variable | Purpose |
|---|---|
| `PLATFORM_API_KEY` | Shared key for platform API calls; required |
| `DB_URL` | JDBC URL of the PostgreSQL database |
| `DB_USER`, `DB_PASS` | Database credentials |
| `PORT` | HTTP port (default `8080`) |
| `ENV` | Runtime environment (`dev`, `prod`) |

See [`.env.example`](.env.example) for development defaults. Never commit real keys or credentials.

## Tests

```bash
./gradlew test
cd demo/ui && npm run check && npm test -- --run
```

## License

MIT — see [`LICENSE`](LICENSE).

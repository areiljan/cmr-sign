# CMR Sign

Open-source service and Kotlin library for adding and verifying **personal electronic signatures** to freight documents such as CMR consignment notes and eFTI records.

It integrates with signing providers rather than issuing certificates. Assurance levels are always derived from standards-based validation, never declared by the caller.

## Who it is for

- **Logistics and eFTI platforms** that need signatures on freight documents without building a signing stack.
- **Carriers, forwarders, shippers and consignees** who sign as natural persons, including one-off signers with no account or certificate.
- **Verifiers and authorities** who need a clear, standards-based verdict on a signed document.
- **Developers** embedding signing or validation in Kotlin/JVM applications through a small library and REST API.

## What it aims to provide

- **Two signer classes** — registered signers using an existing e-ID, and ad hoc signers reached through a single-use, expiring link with provider-led identity proofing.
- **Derived assurance** — the service reports the qualification (AdES/QES) that validation derives from the certificate and trusted lists. Weak identity proofing is reported honestly, never relabelled as a stronger signature.
- **Standards-based validation** — ETSI EN 319 102-1 verdicts via EU DSS, including tamper detection, revocation and trust-chain checks, and long-term validity (B-LT).
- **No key custody** — signers' private keys stay with them or their provider; the service only handles the data to be signed and the resulting signature value.
- **Payload- and format-agnostic core** — CMR PDFs today; structured eFTI datasets, CAdES, XAdES, JAdES and ASiC are planned seams, not assumptions baked into the domain.
- **Simple deployment** — one non-root container, PostgreSQL and a Compose file; configuration comes only from environment variables.

## Project layout

| Module | Purpose |
|---|---|
| `core` | Framework-free domain, state machine, value types, signing and provider interfaces |
| `validation-dss` | EU DSS-backed validator; the only module that imports DSS |
| `service` | HTTP API, PostgreSQL storage, webhooks, signer page |

## Getting started

### Prerequisites

- Docker with Compose (quickest path)
- JDK 21+ for local development and tests

### Run the full stack

```bash
cp .env.example .env
# set a strong PLATFORM_API_KEY in .env
docker compose up --build
```

The service listens on <http://localhost:8080>; the database is exposed on `127.0.0.1:5442`.

Check that it is up:

```bash
curl http://localhost:8080/health
```

API routes live under `/api` and require the `X-Api-Key` header. The health check is public.

### Local development

```bash
docker compose up -d db
./gradlew test
./gradlew :service:run
```

For the UI:

```bash
cd service/ui
npm install
npm run dev
```

### Configuration

| Variable | Purpose |
|---|---|
| `PLATFORM_API_KEY` | Shared key for platform API calls; required |
| `DB_URL` | JDBC URL of the PostgreSQL database |
| `DB_USER`, `DB_PASS` | Database credentials |
| `PORT` | HTTP port (default `8080`) |
| `ENV` | Runtime environment (`dev`, `prod`) |

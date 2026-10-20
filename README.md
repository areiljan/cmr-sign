# CMR Sign

A Kotlin/JVM library and demo for **signing and validating freight documents**
(such as CMR consignment notes) with PAdES signatures and EU DSS validation.
Built around a provider seam, so the signing key can live in a trust service
rather than in your application.

> **Status:** working prototype. Signing and validation run end to end with a
> development identity. Real trust-service integration and eFTI mapping are on
> the [roadmap](#roadmap).

## What works today

- **Sign a PDF** through the `SigningProvider` interface and produce a PAdES-BES signature.
- **Validate with EU DSS 6.5.1**, with distinct, tested outcomes:

  | Case | Verdict |
    |---|---|
  | Valid signature | `TOTAL_PASSED` |
  | Tampered document | `TOTAL_FAILED` / `HASH_FAILURE` |
  | Untrusted chain | `INDETERMINATE` / `NO_CERTIFICATE_CHAIN_FOUND` |
  | Expired certificate | `INDETERMINATE` / `SIG_CONSTRAINTS_FAILURE` |

- **Bind the signature to what was reviewed:** signing uses the document SHA-256 and requires explicit consent.
- **Keep every version:** signed documents are stored as immutable versions; the `ETag` is the SHA-256.
- **Try it in the browser:** create → start → sign → verdict.

## Design

- **Framework-free `core`:** domain, state machine and interfaces, with no web or database dependency.
- **Provider seam:** `SigningProvider` separates signing from key custody.
- **DSS isolated:** only `validation-dss` imports EU DSS (LGPL); the rest stays MIT.
- **Derived, not declared:** the verdict comes from validation, never from the caller.

| Module | Purpose |
|---|---|
| `core` | Domain, state machine, value types, `SigningProvider`/`Validator` interfaces, PAdES |
| `validation-dss` | EU DSS-backed validator |
| `demo` | Klite + PostgreSQL + Svelte demo app |

## Getting started

Prerequisites: JDK 21+ and Docker with Compose.

```bash
cp .env.example .env
# set a strong PLATFORM_API_KEY
docker compose up --build
```

Open <http://localhost:8080>.

Local development:

```bash
docker compose up -d db
./gradlew test
./gradlew :demo:run
# UI: cd demo/ui && npm install && npm run dev  (http://localhost:8000)
```

## Demo flow

1. Upload `core/src/mocks/resources/example-unsigned.pdf`.
2. **Create request** → `CREATED`.
3. **Start session** → `IDENTITY_PROVEN`.
4. Review the SHA-256, confirm consent, then **Sign** → `VALIDATED` with the verdict.
5. Download version 2:

```bash
curl -H "X-Api-Key: $PLATFORM_API_KEY" \
  -o signed.pdf \
  http://localhost:8080/api/envelopes/{id}/documents/2
```

## API

Platform routes require `X-Api-Key`; the health check is public.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/envelopes` | Create a request; JSON body with a base64 `payload` |
| `GET` | `/api/envelopes/{id}` | Read the request state |
| `POST` | `/api/envelopes/{id}/start` | Start the reference signer session |
| `POST` | `/api/envelopes/{id}/sign` | Sign the reviewed hash, validate, return the verdict |
| `GET` | `/api/envelopes/{id}/documents/{version}` | Download a version; `ETag` is the SHA-256 |
| `GET` | `/health` | Liveness |

## Roadmap

- [ ] Provider-agnostic CSC/QTSP adapter and a redesigned `SigningProvider` (certificate chain, algorithm selection, asynchronous authorization)
- [ ] One PDFBox 2.x line and validation of the extracted detached CMS
- [ ] Ad hoc signing hook for platform-owned links
- [ ] `efti` module: derived `eFTI65` / `eFTI115` / `eFTI84` marker and stored validation report
- [ ] Organisational seal (AdESeal) through the same pipeline
- [ ] Revocation checks, webhooks, OpenAPI description

Out of scope: acting as a QTSP, a full eFTI CMDS implementation, long-term validation.

## Limitations

- **Development identity only.** The reference provider is self-signed and keeps a demo key in memory. Its verdict is `NA`, and the output is **not a legally valid eIDAS signature**. Do not use it as one.
- No QTSP, trusted-list or revocation integration yet; validation uses a fixed trust source supplied by the caller.
- Provider sessions are in memory; restarting between start and sign loses the session.

## License

CMR Sign core is MIT-licensed. `validation-dss` depends on EU DSS (LGPL); keep
the third-party notices and comply with the LGPL when distributing a combined build.
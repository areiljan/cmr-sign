# CMR Sign

A Kotlin/JVM library and demo for **signing and validating freight documents**
(such as CMR consignment notes) with PAdES signatures and EU DSS validation.
Built around a provider seam, so the signing key can live in a trust service
rather than in your application.

> **Status:** working prototype. Signing and validation run end to end with a
> development identity. Real trust-service integration and the DigiCMR integration are on
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
- **Derive the eFTI marker:** each request declares its role (`EFTI65_SENDER`, `EFTI115_CARRIER`, `EFTI84_CONSIGNEE`); signing returns and stores the derived status, which attests only `TOTAL_PASSED` with an advanced or qualified level. The development identity gives `NA`, so the demo shows "not attested".
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

### Probe a real CSC provider

`CscProbeTest` signs a PDF through a real CSC API v1 provider and validates it with DSS. It runs only when `CSC_BASE_URL` is set and writes `validation-cades/build/csc-probe-signed.pdf` and `csc-probe-report.xml`.

```bash
CSC_BASE_URL=https://cs-try.ssl.com/csc/v0 \
CSC_TOKEN_URL=https://oauth-sandbox.ssl.com/oauth2/token \
CSC_CLIENT_ID=... CSC_USERNAME=... CSC_PASSWORD=... CSC_CREDENTIAL_ID=... \
./gradlew :validation-cades:test --tests '*CscProbe*' --rerun
```

SSL.com's eSigner sandbox works with its [public demo account](https://www.ssl.com/guide/esigner-demo-credentials-and-certificates/) and its e-seal credential, which needs no OTP. The result is `TOTAL_PASSED` with qualification `NA`: the chain ends at SSL.com's development root, which is not on an EU trusted list.

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
| `POST` | `/api/envelopes` | Create a request; JSON body with a base64 `payload` and an `eftiRole` |
| `GET` | `/api/envelopes/{id}` | Read the request state |
| `POST` | `/api/envelopes/{id}/start` | Start the reference signer session |
| `POST` | `/api/envelopes/{id}/sign` | Sign the reviewed hash, validate, return the verdict and eFTI status |
| `GET` | `/api/envelopes/{id}/documents/{version}` | Download a version; `ETag` is the SHA-256 |
| `GET` | `/api/envelopes/{id}/reports/{reportId}` | Download the stored ETSI validation report (XML) |
| `GET` | `/health` | Liveness |

## Roadmap

- [x] CSC API v1 adapter, probed against SSL.com's eSigner sandbox
- [ ] Redesigned `SigningProvider` (algorithm selection, asynchronous authorization, OTP)
- [ ] One PDFBox 2.x line and validation of the extracted detached CMS
- [ ] Ad hoc signing hook for platform-owned links
- [x] `efti` module: derived `eFTI65` / `eFTI115` / `eFTI84` signature status
- [x] Stored ETSI validation report, referenced by `validationReportId`
- [ ] Organisational seal (AdESeal) through the same pipeline
- [x] Online revocation checks (OCSP/CRL)
- [ ] Webhooks, OpenAPI description

Out of scope: acting as a QTSP, a full eFTI CMDS implementation, long-term validation.

## Limitations

- **Development identity only.** The reference provider is self-signed and keeps a demo key in memory. Its verdict is `NA`, and the output is **not a legally valid eIDAS signature**. Do not use it as one.
- No trusted-list integration yet; validation uses a fixed trust source supplied by the caller and fetches revocation data online (OCSP, falling back to CRL).
- Provider sessions are in memory; restarting between start and sign loses the session.

## License

CMR Sign core is MIT-licensed. `validation-dss` depends on EU DSS (LGPL); keep
the third-party notices and comply with the LGPL when distributing a combined build.
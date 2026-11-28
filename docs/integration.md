# CMR Sign integration

Status: 0.2.1 prototype. Modules and examples: see README.

- Sign: `PadesSigner.forProvider(provider, session, identity).sign(pdf, name)`.
- Validate: `PadesValidator` with `DssCmsValidator` (trust anchor)
  or `DssLotlCmsValidator` (EU LOTL).
- eFTI status: `EftiSignatureStatus.from(...)`; `atLeastAdvanced` is true
  only for `TOTAL_PASSED` with AdES/AdESeal.
- CSC config: `CSC_BASE_URL`, `CSC_TOKEN_URL`, `CSC_CLIENT_ID`,
  `CSC_USERNAME`, `CSC_PASSWORD`, `CSC_CREDENTIAL_ID`.

Limits: one credential, no OTP/async consent, PAdES B-B, no eFTI CMDS write.

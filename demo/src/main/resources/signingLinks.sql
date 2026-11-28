--changeset signingLinks
create table signingLinks(
    ${id},
    envelopeId     text        not null references envelopes (id),
    tokenHash      text        not null unique,
    otpHash        text        not null,
    expiresAt      timestamptz not null,
    failedAttempts int         not null default 0,
    verifiedAt     timestamptz,
    usedAt         timestamptz,
    ${createdAt}
);

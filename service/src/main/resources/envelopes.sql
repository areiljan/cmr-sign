--changeset envelopes
create table envelopes(
    ${id},
    format      text not null,
    signerClass text not null,
    providerSessionId text,
    signerSubject text,
    proofingMethod text,
    state       text not null default 'CREATED',
    ${createdAt}
);

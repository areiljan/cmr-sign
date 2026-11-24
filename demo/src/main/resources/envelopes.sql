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

--changeset envelopes.eftiRole
alter table envelopes add eftiRole text not null default 'EFTI65_SENDER';
alter table envelopes alter eftiRole drop default;

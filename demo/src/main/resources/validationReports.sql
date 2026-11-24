--changeset validationReports
create table validationReports(
    ${id},
    envelopeId      text not null references envelopes (id),
    documentVersion int  not null,
    content         text not null,
    ${createdAt}
);

--changeset validationReports.eftiSignatureType
alter table validationReports add eftiSignatureType text;

--changeset documents
create table documents(
    ${id},
    envelopeId  text        not null references envelopes (id),
    version     int         not null,
    contentType text        not null,
    sha256      text        not null,
    content     bytea       not null,
    ${createdAt},
    unique (envelopeId, version)
);

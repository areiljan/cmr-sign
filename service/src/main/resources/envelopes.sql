--changeset envelopes
create table envelopes(
    ${id},
    format      text not null,
    signerClass text not null,
    state       text not null default 'CREATED',
    ${createdAt}
);

--substitute id=id text primary key
--substitute createdAt=createdAt timestamptz not null default now()

--include envelopes.sql
--include documents.sql

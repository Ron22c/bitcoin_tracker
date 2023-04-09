-- Table: bitcoid.bitcoin_transactions

-- DROP TABLE IF EXISTS bitcoid.bitcoin_transactions;

CREATE TABLE IF NOT EXISTS bitcoid.bitcoin_transactions
(
    id integer NOT NULL DEFAULT nextval('bitcoid.bitcoin_transactions_id_seq'::regclass),
    amount double precision DEFAULT 0,
    total double precision DEFAULT 0,
    created_at timestamp with time zone,
    CONSTRAINT bitcoin_transactions_pkey PRIMARY KEY (id)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS bitcoid.bitcoin_transactions
    OWNER to postgres;
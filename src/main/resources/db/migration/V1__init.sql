CREATE TABLE users (
    id                uuid PRIMARY KEY,
    email             text        NOT NULL,
    password_hash     text,
    email_verified_at timestamptz,
    display_name      varchar(80),
    locale            varchar(5)  NOT NULL DEFAULT 'en' CHECK (locale IN ('en', 'es')),
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_email_lower_uq ON users (lower(email));

CREATE TABLE user_identities (
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider   varchar(16) NOT NULL CHECK (provider IN ('GOOGLE', 'APPLE')),
    subject    text        NOT NULL,
    email      text,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, subject)
);
CREATE INDEX user_identities_user_idx ON user_identities (user_id);

CREATE TABLE refresh_tokens (
    id          uuid PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id   uuid        NOT NULL,
    token_hash  char(64)    NOT NULL UNIQUE,
    expires_at  timestamptz NOT NULL,
    revoked_at  timestamptz,
    replaced_by uuid,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX refresh_tokens_family_idx ON refresh_tokens (family_id);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);

CREATE TABLE email_tokens (
    id            uuid PRIMARY KEY,
    user_id       uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash    char(64)    NOT NULL UNIQUE,
    purpose       varchar(8)  NOT NULL CHECK (purpose IN ('VERIFY', 'RESET')),
    password_hash text,
    expires_at    timestamptz NOT NULL,
    used_at       timestamptz,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX email_tokens_user_idx ON email_tokens (user_id, purpose);

CREATE TABLE user_images (
    key        text PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    url        text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX user_images_user_idx ON user_images (user_id);

CREATE TABLE avatars (
    user_id    uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    photo_url  text        NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE clothes (
    id          uuid PRIMARY KEY,
    user_id     uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category    varchar(16)  NOT NULL,
    image_url   text         NOT NULL,
    name        varchar(100),
    colour      varchar(50),
    pattern     varchar(50),
    formality   varchar(16),
    warmth      varchar(16),
    description varchar(500),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX clothes_user_idx ON clothes (user_id, created_at);

CREATE TABLE looks (
    id           uuid PRIMARY KEY,
    user_id      uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event        varchar(200) NOT NULL,
    status       varchar(16)  NOT NULL CHECK (status IN ('QUEUED', 'PICKING', 'RENDERING', 'READY', 'FAILED')),
    clothe_ids   uuid[]       NOT NULL DEFAULT '{}',
    stylist_note text,
    image_url    text,
    saved        boolean      NOT NULL DEFAULT false,
    failure_code varchar(32),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX looks_user_idx ON looks (user_id, created_at DESC);
CREATE INDEX looks_in_progress_idx ON looks (updated_at) WHERE status IN ('QUEUED', 'PICKING', 'RENDERING');

CREATE TABLE wallet_buckets (
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    bucket     varchar(8)  NOT NULL CHECK (bucket IN ('PLAN', 'FREE', 'REWARD', 'PACK')),
    balance    integer     NOT NULL DEFAULT 0 CHECK (balance >= 0),
    expires_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, bucket)
);

CREATE TABLE credit_ledger (
    id              uuid PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    delta           integer     NOT NULL,
    bucket          varchar(8)  NOT NULL CHECK (bucket IN ('PLAN', 'FREE', 'REWARD', 'PACK')),
    reason          varchar(24) NOT NULL,
    ref_id          text,
    idempotency_key text        NOT NULL UNIQUE,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX credit_ledger_user_idx ON credit_ledger (user_id, created_at DESC);
CREATE INDEX credit_ledger_ref_idx ON credit_ledger (ref_id);

CREATE FUNCTION credit_ledger_append_only() RETURNS trigger AS
$$
BEGIN
    RAISE EXCEPTION 'credit_ledger is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER credit_ledger_no_update
    BEFORE UPDATE ON credit_ledger
    FOR EACH ROW EXECUTE FUNCTION credit_ledger_append_only();

CREATE TABLE credit_holds (
    ref_id     text PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount     integer     NOT NULL CHECK (amount > 0),
    status     varchar(10) NOT NULL CHECK (status IN ('RESERVED', 'CONFIRMED', 'REFUNDED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX credit_holds_user_idx ON credit_holds (user_id);

CREATE TABLE billing_customers (
    user_id            uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    stripe_customer_id text        NOT NULL UNIQUE,
    created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id                       uuid PRIMARY KEY,
    user_id                  uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider                 varchar(8)  NOT NULL,
    provider_subscription_id text UNIQUE,
    plan_code                varchar(16) NOT NULL,
    status                   varchar(16) NOT NULL,
    current_period_end       timestamptz,
    cancel_at_period_end     boolean     NOT NULL DEFAULT false,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX subscriptions_user_idx ON subscriptions (user_id);

CREATE TABLE payments (
    id           uuid PRIMARY KEY,
    user_id      uuid        REFERENCES users (id) ON DELETE SET NULL,
    provider     varchar(8)  NOT NULL,
    product_code varchar(16) NOT NULL,
    amount       bigint      NOT NULL,
    currency     varchar(3)  NOT NULL,
    status       varchar(10) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'FAILED')),
    provider_ref text UNIQUE,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX payments_user_idx ON payments (user_id);

CREATE TABLE webhook_events (
    provider    varchar(8)  NOT NULL,
    event_id    text        NOT NULL,
    event_type  text        NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (provider, event_id)
);

CREATE TABLE ai_usage (
    id            uuid PRIMARY KEY,
    user_id       uuid REFERENCES users (id) ON DELETE SET NULL,
    operation     varchar(32)  NOT NULL,
    model         varchar(100) NOT NULL,
    input_tokens  integer      NOT NULL,
    output_tokens integer      NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ai_usage_user_idx ON ai_usage (user_id, created_at);

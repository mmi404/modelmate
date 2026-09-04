create extension if not exists pg_trgm;

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
create table users (
    id            bigint generated always as identity primary key,
    first_name    varchar(100) not null,
    last_name     varchar(100) not null,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(20)  not null default 'USER',
    avatar_url    varchar(500),
    bio           text,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),
    constraint users_role_chk check (role in ('USER', 'ADMIN'))
);

-- ---------------------------------------------------------------------------
-- categories
-- ---------------------------------------------------------------------------
create table categories (
    id           bigint generated always as identity primary key,
    name         varchar(255) not null unique,
    slug         varchar(255) not null unique,
    description  text,
    applications text,
    created_at   timestamptz  not null default now()
);

-- ---------------------------------------------------------------------------
-- models  (submissions + approved models, distinguished by status)
-- ---------------------------------------------------------------------------
create table models (
    id               bigint generated always as identity primary key,
    name             varchar(255) not null,
    slug             varchar(255) not null unique,
    creator          varchar(255),
    category_id      bigint not null references categories (id),
    description      text,
    website_url      varchar(500),
    logo_url         varchar(500),
    status           varchar(20) not null default 'PENDING',
    submitted_by     bigint not null references users (id),
    approved_by      bigint references users (id),
    rejection_reason text,
    created_at       timestamptz not null default now(),
    approved_at      timestamptz,
    constraint models_status_chk check (status in ('PENDING', 'APPROVED', 'REJECTED'))
);
create index idx_models_status_category on models (status, category_id);
create index idx_models_name_trgm on models using gin (name gin_trgm_ops);

-- ---------------------------------------------------------------------------
-- reviews  (type = REVIEW | PROBLEM)
-- ---------------------------------------------------------------------------
create table reviews (
    id             bigint generated always as identity primary key,
    model_id       bigint not null references models (id),
    user_id        bigint not null references users (id),
    type           varchar(10) not null,
    title          varchar(255),
    content        text not null,
    accuracy       smallint,
    speed          smallint,
    cost           smallint,
    ease_of_use    smallint,
    reliability    smallint,
    overall_rating numeric(3, 2),
    severity       varchar(10),
    status         varchar(10) not null default 'VISIBLE',
    upvote_count   integer not null default 0,
    downvote_count integer not null default 0,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    constraint reviews_type_chk check (type in ('REVIEW', 'PROBLEM')),
    constraint reviews_status_chk check (status in ('VISIBLE', 'HIDDEN')),
    constraint reviews_severity_chk
        check (severity is null or severity in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    constraint reviews_rating_range_chk check (
        (accuracy    is null or accuracy    between 1 and 5) and
        (speed       is null or speed       between 1 and 5) and
        (cost        is null or cost        between 1 and 5) and
        (ease_of_use is null or ease_of_use between 1 and 5) and
        (reliability is null or reliability between 1 and 5)
    ),
    constraint reviews_review_needs_ratings_chk check (
        type <> 'REVIEW' or (
            accuracy is not null and speed is not null and cost is not null
                and ease_of_use is not null and reliability is not null
        )
    )
);
create unique index uq_reviews_one_review_per_user_model
    on reviews (model_id, user_id) where type = 'REVIEW';
create index idx_reviews_model_type_status on reviews (model_id, type, status);

-- ---------------------------------------------------------------------------
-- discussions + tags + replies
-- ---------------------------------------------------------------------------
create table discussions (
    id             bigint generated always as identity primary key,
    title          varchar(500) not null,
    content        text not null,
    author_id      bigint not null references users (id),
    reply_count    integer not null default 0,
    upvote_count   integer not null default 0,
    downvote_count integer not null default 0,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create table discussion_tags (
    discussion_id bigint not null references discussions (id) on delete cascade,
    tag           varchar(50) not null,
    primary key (discussion_id, tag)
);
create index idx_discussion_tags_tag on discussion_tags (tag);

create table replies (
    id              bigint generated always as identity primary key,
    discussion_id   bigint not null references discussions (id) on delete cascade,
    parent_reply_id bigint references replies (id) on delete cascade,
    author_id       bigint not null references users (id),
    content         text not null,
    upvote_count    integer not null default 0,
    downvote_count  integer not null default 0,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);
create index idx_replies_discussion on replies (discussion_id);

-- ---------------------------------------------------------------------------
-- votes  (polymorphic target)
-- ---------------------------------------------------------------------------
create table votes (
    id          bigint generated always as identity primary key,
    user_id     bigint not null references users (id),
    target_type varchar(12) not null,
    target_id   bigint not null,
    value       smallint not null,
    created_at  timestamptz not null default now(),
    constraint votes_target_type_chk check (target_type in ('DISCUSSION', 'REPLY', 'REVIEW')),
    constraint votes_value_chk check (value in (1, -1)),
    constraint uq_votes_user_target unique (user_id, target_type, target_id)
);

-- ---------------------------------------------------------------------------
-- password_reset_tokens
-- ---------------------------------------------------------------------------
create table password_reset_tokens (
    id            bigint generated always as identity primary key,
    user_id       bigint not null references users (id),
    code_hash     varchar(255) not null,
    expires_at    timestamptz not null,
    used          boolean not null default false,
    attempt_count integer not null default 0,
    created_at    timestamptz not null default now()
);
create index idx_prt_user on password_reset_tokens (user_id);

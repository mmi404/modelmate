# ModelMate — Data Model

PostgreSQL 16. All PKs `bigint generated always as identity`. All timestamps
`timestamptz` in UTC. Managed by Flyway (`backend/src/main/resources/db/migration`).

Derived from `version1.0` (entities + `database/init.sql`) and the project
proposal, with these **cleanups**:

- `model_submissions` is **merged into `models`** via a `status` column
  (`PENDING | APPROVED | REJECTED`) — no duplicate table.
- Rating dimensions become `smallint` `1..5` (were `varchar(50)`), aligned to the
  proposal's five criteria: **accuracy, speed, cost, ease_of_use, reliability**.
- `votes` uses a **polymorphic `(target_type, target_id)`** pair with a single
  unique constraint (was three nullable FK columns).
- Reviews and problem reports share one table via `type` (`REVIEW | PROBLEM`).
- Denormalised counters (`reply_count`, `upvote_count`, …) are maintained in the
  service layer inside the same transaction as the mutating action.
- `categories.model_count` is **not stored** — computed via query.

---

## Entities

### `users`
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| first_name | varchar(100) | not null |
| last_name | varchar(100) | not null |
| email | varchar(255) | not null, unique, citext-lower |
| password_hash | varchar(255) | not null, BCrypt |
| role | varchar(20) | not null, default `USER` (`USER`\|`ADMIN`) |
| avatar_url | varchar(500) | nullable |
| bio | text | nullable |
| created_at | timestamptz | not null, default now() |
| updated_at | timestamptz | not null |

### `categories`
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| name | varchar(255) | not null, unique |
| slug | varchar(255) | not null, unique |
| description | text | |
| applications | text | comma/'. '-separated use cases |
| created_at | timestamptz | not null |

Seed: the 12 categories from `version1.0/database/init.sql` (NLP, Computer Vision,
Speech & Audio, Multimodal, Recommendation Systems, Generative Models, Time-Series
Forecasting, Robotics & Control, Reinforcement Learning, Anomaly Detection,
Graph/Network Models, Code & Programming Assistants).

### `models`
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| name | varchar(255) | not null |
| slug | varchar(255) | not null, unique |
| creator | varchar(255) | nullable (e.g. "OpenAI") |
| category_id | bigint FK→categories | not null |
| description | text | |
| website_url | varchar(500) | nullable, normalised to include scheme |
| logo_url | varchar(500) | nullable |
| status | varchar(20) | not null, default `PENDING` (`PENDING`\|`APPROVED`\|`REJECTED`) |
| submitted_by | bigint FK→users | not null |
| approved_by | bigint FK→users | nullable |
| rejection_reason | text | nullable |
| created_at | timestamptz | not null |
| approved_at | timestamptz | nullable |

Indexes: `(status, category_id)`, `(slug)`, GIN trigram on `name` for search.
Only `status = APPROVED` rows are publicly visible.

Seed: the ~12 models from `init.sql` (GPT-4, BERT, LLaMA, YOLOv8, ViT, SAM,
Whisper, DeepSpeech, Bark, GPT-4o, CLIP, Flamingo) as `APPROVED`.

### `reviews`  (reviews **and** problem reports)
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| model_id | bigint FK→models | not null |
| user_id | bigint FK→users | not null |
| type | varchar(10) | not null (`REVIEW`\|`PROBLEM`) |
| title | varchar(255) | nullable (used by PROBLEM) |
| content | text | not null |
| accuracy | smallint | 1..5, null for PROBLEM |
| speed | smallint | 1..5, null for PROBLEM |
| cost | smallint | 1..5, null for PROBLEM |
| ease_of_use | smallint | 1..5, null for PROBLEM |
| reliability | smallint | 1..5, null for PROBLEM |
| overall_rating | numeric(3,2) | computed avg of the 5 on write; null for PROBLEM |
| severity | varchar(10) | `LOW`\|`MEDIUM`\|`HIGH`\|`CRITICAL`, only for PROBLEM |
| status | varchar(10) | `VISIBLE`\|`HIDDEN` (moderation), default `VISIBLE` |
| upvote_count | int | default 0 |
| downvote_count | int | default 0 |
| created_at | timestamptz | not null |
| updated_at | timestamptz | not null |

Constraint: unique `(model_id, user_id)` where `type = 'REVIEW'`
(one review per user per model; multiple PROBLEMs allowed).
Check: if `type='REVIEW'` then the five rating columns are all non-null.

A model's aggregate rating = `avg(overall_rating)` over `VISIBLE REVIEW` rows;
per-dimension averages computed the same way. Used by model detail + leaderboard.

### `discussions`
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| title | varchar(500) | not null |
| content | text | not null |
| author_id | bigint FK→users | not null |
| reply_count | int | default 0 |
| upvote_count | int | default 0 |
| downvote_count | int | default 0 |
| created_at | timestamptz | not null |
| updated_at | timestamptz | not null |

### `discussion_tags`
| column | type | notes |
|---|---|---|
| discussion_id | bigint FK→discussions | on delete cascade |
| tag | varchar(50) | |
PK `(discussion_id, tag)`. Index on `tag` for filtering + tag cloud.

### `replies`
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| discussion_id | bigint FK→discussions | not null, on delete cascade |
| parent_reply_id | bigint FK→replies | nullable (one level of threading) |
| author_id | bigint FK→users | not null |
| content | text | not null |
| upvote_count | int | default 0 |
| downvote_count | int | default 0 |
| created_at | timestamptz | not null |
| updated_at | timestamptz | not null |

### `votes`
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| user_id | bigint FK→users | not null |
| target_type | varchar(12) | `DISCUSSION`\|`REPLY`\|`REVIEW` |
| target_id | bigint | not null |
| value | smallint | `1` or `-1` |
| created_at | timestamptz | not null |
Unique `(user_id, target_type, target_id)`. Changing a vote updates `value`;
removing it deletes the row. Counter columns on the target are adjusted in the
same transaction.

### `password_reset_tokens`
| column | type | notes |
|---|---|---|
| id | bigint PK | |
| user_id | bigint FK→users | not null |
| code_hash | varchar(255) | not null, hash of 6-digit code |
| expires_at | timestamptz | not null (now + 15 min) |
| used | boolean | default false |
| attempt_count | int | default 0 (lock after 5) |
| created_at | timestamptz | not null |
Index `(user_id)`; latest unused row wins; older rows purged on new request.

---

## Relationship summary

```
users 1───* models          (submitted_by)
users 1───* reviews
users 1───* discussions
users 1───* replies
users 1───* votes
users 1───* password_reset_tokens
categories 1───* models
models 1───* reviews
discussions 1───* replies
discussions 1───* discussion_tags
replies 0..1───* replies      (parent_reply_id, single level)
votes *───1 (discussion | reply | review)   via target_type/target_id
```

## Read models / derived data

| Need | Query |
|---|---|
| Category model count | `count(models) where status=APPROVED group by category_id` |
| Model card rating | `avg(overall_rating), count(*) from reviews where model_id=? and type='REVIEW' and status='VISIBLE'` |
| Leaderboard | models ordered by `avg(overall_rating)` then `count(reviews)`, optional category filter, `having count >= N` |
| Trending | approved models ordered by review count in the last 30 days, then recency |
| Community stats | distinct authors, total discussions, total replies |
| Profile contributions | union of that user's reviews + discussions + replies, newest first |

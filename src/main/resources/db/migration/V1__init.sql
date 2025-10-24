create table org (
  id bigserial primary key,
  name varchar(255) not null,
  plan varchar(50),
  stripe_customer_id varchar(255),
  created_at timestamptz default now()
);

create table app_user (
  id bigserial primary key,
  org_id bigint not null references org(id),
  email varchar(255) not null unique,
  role varchar(20) not null default 'MEMBER',
  active boolean default true
);

create table event (
  id bigserial primary key,
  org_id bigint not null references org(id),
  name varchar(255) not null,
  starts_at timestamptz not null,
  cutoff_at timestamptz,
  notes text
);

create table menu_item (
  id bigserial primary key,
  event_id bigint not null references event(id),
  name varchar(255) not null,
  description text,
  price_cents integer,
  cap_qty integer,
  station varchar(100)
);

create table preorder (
  id bigserial primary key,
  event_id bigint not null references event(id),
  user_id bigint not null references app_user(id),
  status varchar(20) not null default 'CREATED',
  created_at timestamptz default now()
);

create table preorder_item (
  id bigserial primary key,
  preorder_id bigint not null references preorder(id) on delete cascade,
  menu_item_id bigint not null references menu_item(id),
  qty integer not null default 1
);

create table otp_code (
  id bigserial primary key,
  email varchar(255) not null,
  code varchar(20) not null,
  expires_at timestamptz not null
);

alter table org
  add column if not exists stripe_subscription_id varchar(255),
  add column if not exists subscription_status varchar(50);

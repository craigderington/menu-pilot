alter table event
  add column if not exists cover_image_path varchar(1024),
  add column if not exists theme_accent_hex varchar(16);

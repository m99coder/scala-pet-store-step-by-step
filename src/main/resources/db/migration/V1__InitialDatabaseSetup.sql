CREATE TABLE pets(
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR NOT NULL,
  category VARCHAR NOT NULL,
  bio VARCHAR NOT NULL,
  status VARCHAR NOT NULL,
  photo_urls VARCHAR NOT NULL,
  tags VARCHAR NOT NULL
);

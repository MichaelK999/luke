-- source of truth for tables in our sql-lite

-- User table
CREATE TABLE IF NOT EXISTS app_user (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    accessibility BOOLEAN DEFAULT 0
);

-- Favourite user path table (stores favourite routes with from/to locations)
CREATE TABLE IF NOT EXISTS favourite_user_path (
    user_id INTEGER NOT NULL,
    from_location TEXT NOT NULL,
    to_location TEXT NOT NULL,
    PRIMARY KEY (user_id, from_location, to_location),
    FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

-- Location table
CREATE TABLE IF NOT EXISTS location (
    location_id INTEGER PRIMARY KEY AUTOINCREMENT,
    location_name TEXT NOT NULL,
    is_closed BOOLEAN DEFAULT 0,
    path_id INTEGER
);

-- Path table
CREATE TABLE IF NOT EXISTS path (
    path_id INTEGER PRIMARY KEY AUTOINCREMENT,
    begin_location_id INTEGER NOT NULL,
    end_location_id INTEGER NOT NULL,
    is_closed BOOLEAN DEFAULT 0,
    is_accessible BOOLEAN DEFAULT 1,
    FOREIGN KEY (begin_location_id) REFERENCES location(location_id),
    FOREIGN KEY (end_location_id) REFERENCES location(location_id)
);

-- User table
CREATE TABLE IF NOT EXISTS app_user (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    fav_path_id INTEGER,
    disabled BOOLEAN DEFAULT 0,
    FOREIGN KEY (fav_path_id) REFERENCES path(path_id)
);

-- Favourite user location table
CREATE TABLE IF NOT EXISTS favourite_user_location (
    user_id INTEGER NOT NULL,
    location_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, location_id),
    FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES location(location_id) ON DELETE CASCADE
);

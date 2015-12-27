CREATE TABLE IF NOT EXISTS user_profile (
	id        BIGINT(20)  NOT NULL,
	email     VARCHAR(45) NULL DEFAULT NULL,
	phone     VARCHAR(45) NULL DEFAULT NULL,
	address   VARCHAR(60) NULL DEFAULT NULL,
	birthdate DATE        NULL DEFAULT NULL,
	PRIMARY KEY (id),
	UNIQUE INDEX email_UNIQUE (email ASC)
);

ALTER TABLE user ADD COLUMN password CHAR(60) BINARY NULL DEFAULT NULL
AFTER balance;

ALTER TABLE user ADD COLUMN user_profile_id BIGINT(20) NULL DEFAULT NULL
AFTER password;

ALTER TABLE user ADD CONSTRAINT fk_user_user_profile
FOREIGN KEY (user_profile_id)
REFERENCES user_profile (id)
	ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS role (
	id   BIGINT(20)  NOT NULL AUTO_INCREMENT,
	name VARCHAR(45) NOT NULL,
	PRIMARY KEY (id),
	UNIQUE INDEX name_UNIQUE (name ASC)
);

CREATE TABLE IF NOT EXISTS permission (
	id   BIGINT(20)  NOT NULL AUTO_INCREMENT,
	name VARCHAR(45) NOT NULL,
	PRIMARY KEY (id),
	UNIQUE INDEX name_UNIQUE (name ASC)
);

CREATE TABLE IF NOT EXISTS role_permissions (
	role_id       BIGINT(20) NOT NULL,
	permission_id BIGINT(20) NOT NULL,
	PRIMARY KEY (role_id, permission_id),
	INDEX fk_role_permission_permission_idx (permission_id ASC),
	CONSTRAINT fk_role_permission_role
	FOREIGN KEY (role_id)
	REFERENCES role (id)
		ON DELETE CASCADE
		ON UPDATE NO ACTION,
	CONSTRAINT fk_role_permission_permission
	FOREIGN KEY (permission_id)
	REFERENCES permission (id)
		ON DELETE CASCADE
		ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS user_roles (
	user_id BIGINT(20) NOT NULL,
	role_id BIGINT(20) NOT NULL,
	PRIMARY KEY (user_id, role_id),
	INDEX fk_user_role_role_idx (role_id ASC),
	CONSTRAINT fk_user_role_user
	FOREIGN KEY (user_id)
	REFERENCES user (id)
		ON DELETE CASCADE
		ON UPDATE NO ACTION,
	CONSTRAINT fk_user_role_role
	FOREIGN KEY (role_id)
	REFERENCES role (id)
		ON DELETE CASCADE
		ON UPDATE NO ACTION
);

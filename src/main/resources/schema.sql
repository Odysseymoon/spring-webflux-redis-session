DROP TABLE IF EXISTS `user`;

CREATE TABLE IF NOT EXISTS `user` (
  `user_id`         varchar(30)  NOT NULL COMMENT '사용자아이디',
  `password`        varchar(100) NOT NULL COMMENT '비밀번호',
  `update_date`     timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
);
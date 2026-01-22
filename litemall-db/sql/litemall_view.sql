CREATE VIEW view_litemall_trader
AS
SELECT t.*, CONCAT(c.nickname, '(', c.username, ')') as creator_name, CONCAT(u.nickname, '(', u.username, ')')as director_name FROM  litemall_trader t 
LEFT JOIN litemall_user u ON t.user_id = u.id
LEFT JOIN litemall_user c ON c.id = t.creator_id;

CREATE VIEW view_litemall_user
AS
SELECT u.*, t.nickname as default_trader_name FROM  litemall_user u
LEFT JOIN litemall_trader t ON u.default_trader_id = t.id;
CREATE VIEW view_litemall_trader
AS
SELECT t.*, c.nickname as creator_name, u.nickname as director_name FROM  litemall_trader t 
LEFT JOIN litemall_user u ON t.user_id = u.id
LEFT JOIN litemall_user c ON c.id = t.creator_id
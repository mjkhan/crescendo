use crsnd_admin;

set @user_id := 'crescendo';
set @alias := @user_id;
set @passwd := @user_id;
set @img_url := null;
set @now := now();

delete from crsnd_account where user_id = @user_id;
insert into crsnd_account (user_id, alias, passwd, img_url, ins_time, upd_time, status)
values (@user_id, @alias, @passwd, @img_url, @now, @now, '001');

delete from crsnd_account_attr where user_id = @user_id;
insert into crsnd_account_attr(user_id, attr_key, attr_val) values (@user_id, 'system-administrator', 'true');

set @site_id := 'crescendo';
set @site_name := @site_id;
set @site_type := null;
set @site_spc := null;
set @db_conn := 'crsnd_site';
set @rd_conn := @db_conn;
set @filebase := null;
set @profile := null;
set @evt_cfg := 'crescendo-event';
set @job_cfg := 'crescendo-job';
set @ui_ctx := 'test/destination.xml';
set @owner_id := @user_id;
set @owner_name := @alias;

delete from crsnd_site where site_id = @site_id;
insert into crsnd_site (
    site_id, site_name, site_type, site_spc, db_conn, rd_conn, filebase, profile, evt_cfg, job_cfg, ui_ctx,
    ins_id, ins_name, owner_id, owner_name, ins_time, upd_time, status
) values (
    @site_id, @site_name, @site_type, @site_spc, @db_conn, @rd_conn, @filebase, @profile, @evt_cfg, @job_cfg, @ui_ctx,
    @owner_id, @owner_name, @owner_id, @owner_name, @now, @now, '001'
);

/*
show tables;

select * from crsnd_common_code;
select * from crsnd_account where user_id = 'test';
select * from crsnd_site where site_id = 'test';

delete from crsnd_site where site_id = 'test';
delete from crsnd_account where user_id = 'test';
*/
select * from crsnd_common_code where cd_grp = 'account' and code = 'reserved-id'
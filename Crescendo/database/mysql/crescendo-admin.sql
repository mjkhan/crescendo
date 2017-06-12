use crsnd_admin;

show tables;

drop table if exists crsnd_dummy;
create table crsnd_dummy (
    dummy                varchar(5)       null
) comment='dummy table';

insert into crsnd_dummy (dummy) values('dummy');

drop table if exists crsnd_group;
create table crsnd_group (
    site_id              varchar(32)  not null comment 'site id',
    grp_type             varchar(11)  not null comment 'group type(appication-defined)',
    grp_id               varchar(11)  not null comment 'group id',
    subtype              varchar(11)      null comment 'group sub-type(application-defined)',
    grp_name             varchar(64)      null comment 'group name',
    descrp               varchar(128)     null comment 'description',
    prnt_id              varchar(11)      null comment 'id of the parent group',
    img_url              varchar(256)     null comment 'url to the group''s image file',
    sort_ord             int unsigned     null comment 'sort order in the parent group',
    owner_type           varchar(11)      null comment 'owner type(application-defined)',
    owner_id             varchar(32)      null comment 'owner id',
    ins_id               varchar(32)  not null comment 'id of the account that created the group',
    ins_time             datetime     not null comment 'datetime when the group is created',
    status               varchar(3)   not null default '001' comment '000:created, 001:active, 002:inactive, 998:removed, 999:delete',
    primary key (site_id, grp_type, grp_id),
    index (site_id, grp_type, prnt_id, owner_type, owner_id, status)
) comment='generic group info';

drop table if exists crsnd_group_member;
create table crsnd_group_member (
    site_id              varchar(32)  not null comment 'site id',
    grp_type             varchar(11)  not null comment 'group type(application-defined)',
    grp_id               varchar(11)  not null comment 'group id',
    mb_type              varchar(11)  not null comment 'member type(application-defined)',
    mb_id                varchar(11)  not null comment 'member id',
    sort_ord             int unsigned     null comment 'sort order',
    ins_id               varchar(32)  not null comment 'id of the account that created the membership',
    ins_time             datetime     not null comment 'datetime when the group is created',
    primary key (site_id, grp_type, grp_id, mb_type, mb_id)
) comment='group membership info';

drop table if exists crsnd_common_code;
create table crsnd_common_code (
    grp_id               varchar(11)  not null comment 'ID of the code''s group',
    code                 varchar(64)  not null comment 'code',
    code_val             varchar(256) not null comment 'code value',
    index (cd_grp, code)
) comment='common code';

insert into crsnd_common_code (cd_grp, code, code_val)
select 'account', 'account', '' union
select 'site', 'site', '' union
select 'account', 'reserved-id', 'unknown' union
select 'site', 'reserved-id', 'unknown';

drop table if exists crsnd_action;
create table crsnd_action (
    action_grp           varchar(32)  not null comment 'action group(application-defined)',
    action_id            varchar(32)  not null comment 'action id',
    action_target        varchar(16)  not null comment 'action target(application-defined)',
    action_name          varchar(32)  not null comment 'action name',
    descrp               text             null comment 'description',
    sort_ord             int         default 0 comment 'sort order with respect to an action target',
    primary key (action_grp, action_id, action_target)
) comment='action info';

insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'admin', 'crescendo', 'Admin Crescendo', 'Administers Crescendo');

drop table if exists crsnd_account;
create table crsnd_account (
    user_id              varchar(32)  not null comment 'account id',
    user_type            varchar(16)      null comment 'account type(application-defined)',
    alias                varchar(64)      null comment 'account alias',
    passwd               varchar(32)  not null comment 'account password',
    img_url              varchar(256)     null comment 'url to the account''s image file',
    ins_time             datetime     not null comment 'datetime when the account is created',
    upd_time             datetime     not null comment 'datetime when the account is last modified',
    status               varchar(3)   not null comment '000:created, 001:active, 002:inactive, 998:removed, 999:delete',
    primary key (user_id)
) comment='account info';

drop table if exists crsnd_account_attr;
create table crsnd_account_attr (
    user_id              varchar(32)  not null comment 'account id',
    attr_key             varchar(64)  not null comment 'attribute key',
    attr_val             text             null comment 'attribute value',
    primary key (user_id, attr_key)
) comment='account attributes extending the account''s fields';

insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'search', 'account', 'Search accounts', 'Searches account information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'get', 'account', 'Get account info', 'Gets account information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'view', 'account', 'View account info', 'Views account information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'new', 'account', 'New account info', 'New account information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'create', 'account', 'Create account', 'Creates an account');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'update', 'account', 'Update account', 'Updates account information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'remove', 'account', 'Remove account', 'Removes account information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'change-status', 'account', 'Change account status', 'Changes account status');

drop table if exists crsnd_site;
create table crsnd_site (
    site_id              varchar(32)  not null comment 'site id',
    site_type            varchar(16)      null comment 'site type(application-defined)',
    site_name            varchar(64)      null comment 'site name',
    site_spc             varchar(20)      null comment 'site space',
    db_conn              varchar(20)  not null comment 'config name of the connection to the read-write database',
    rd_conn              varchar(20)      null comment 'config name of the connection to the read-only database',
    filebase             varchar(128)     null comment 'file path to the filebase configuration',
    profile              varchar(128)     null comment 'file path to the site profile',
    evt_cfg              varchar(64)      null comment 'name of the event configuration',
    job_cfg              varchar(64)      null comment 'name of the job configuration',
    ui_ctx               varchar(64)      null comment 'name of the UI context',
    ins_id               varchar(32)  not null comment 'id of the account that created the site',
    ins_name             varchar(64)  not null comment 'alias of the account that created the site',
    owner_id             varchar(32)  not null comment 'id of the account that owns the site',
    owner_name           varchar(64)  not null comment 'alias of the account that owns the site',
    ins_time             datetime     not null comment 'datetime when the site is created',
    upd_time             datetime     not null comment 'datetime when the site is last modified',
    status               varchar(3)   not null default '000' comment '000:created, 001:active, 002:inactive, 998:removed, 999:delete',
    primary key (site_id)
) comment='site info';

drop table if exists crsnd_site_attr;
create table crsnd_site_attr (
    site_id              varchar(32)  not null comment 'site id',
    attr_key             varchar(64)  not null comment 'attribute key',
    attr_val             text             null comment 'attribute value',
    primary key (site_id, attr_key)
) comment='site attributes extending the site''s fields';

insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'access', 'site', 'Access site', 'Accesses a site for actions');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'search', 'site', 'Search sites', 'Searches site information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'get', 'site', 'Get site info', 'Gets site information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'view', 'site', 'View site info', 'Views site information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'new', 'site', 'New site info', 'New site information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'create', 'site', 'Create site', 'Creates a site');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'update', 'site', 'Update site', 'Updates site information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'remove', 'site', 'Remove site', 'Removes site information');
insert into crsnd_action (action_grp, action_id, action_target, action_name, descrp)
values ('default', 'change-status', 'site', 'Change site status', 'Changes site status');

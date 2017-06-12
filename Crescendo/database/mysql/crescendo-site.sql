use crsnd_site;

show tables;

drop table if exists crsnd_dummy;
create table crsnd_dummy (
    dummy                varchar(5)       null
) comment='dummy table';

insert into crsnd_dummy (dummy) values('dummy');

drop table if exists crsnd_permission;
create table crsnd_permission (
    site_id              varchar(32)  not null comment 'site id',
    action_id            varchar(32)  not null comment 'action id',
    action_target        varchar(16)  not null comment 'action target(application-defined)',
    target_id            varchar(128) not null default '*' comment 'id of the action target',
    pctx_type            varchar(16)  not null default '*' comment 'type of context for permission evaluation(application-defined)',
    pctx_id              varchar(32)  not null default '*' comment 'id of context for permission evaluation',
    user_type            varchar(3)   not null default '001' comment 'principal type, 000: account, 001: user group, 002: ip address, 003: device identifier',
    user_id              varchar(128) not null default '*' comment 'principal id',
    primary key (site_id, action_id, action_target, target_id, pctx_type, pctx_id, user_type, user_id)
) comment='site''s action permission on targets by principals';

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
    mb_id                varchar(11)  not null comment 'member id'
    sort_ord             int unsidned     null comment 'sort order',
    ins_id               varchar(32)  not null comment 'id of the account that created the membership',
    ins_time             datetime     not null comment 'datetime when the group is created',
    status               varchar(3)   not null default '001' comment '000:created, 001:active, 002:inactive, 998:removed, 999:delete',
    primary key (site_id, grp_type, grp_id, mbm_type, mbm_id)
) comment='group membership info';

drop table if exists crsnd_site_user;
create table crsnd_site_user (
    site_id              varchar(32)  not null comment 'site id',
    grp_id               varchar(11)  not null comment 'usergroup id',
    user_type            varchar(16)  not null default '000' comment 'user type, 000:account, 001:, 002:ip address, 003: device',
    user_id              varchar(32)  not null comment 'user id',
    user_name            varchar(64)      null comment 'user name or alias',
    img_url              varchar(256)     null comment 'url to the user''s image file',
    sort_ord             int unsigned default 0 comment 'sort order',
    ins_time             datetime     not null comment 'datetime when the user is added to the usergroup',
    primary key (site_id, grp_id, user_type, user_id)
) comment='user info in site''s usergroups';

drop table if exists crsnd_menu;
create table crsnd_menu (
    site_id              varchar(32)  not null comment 'site id',
    menu_id              varchar(5)   not null comment 'menu id',
    menu_type            varchar(16)  not null default 'default' comment 'menu type(application-defined)',
/*
    sub_type             varchar(16)      null comment 'menu sub-type(application-defined)',
*/
    prnt_id              varchar(5)       null comment 'id of the parent menu',
    menu_name            varchar(128) not null comment 'menu name',
    descrp               text             null comment 'description',
    menu_action          varchar(256)     null comment 'string to invoke the menu''s action',
    setting              varchar(128)     null comment 'setting for the menu''s action(application-defined)',
    img_url              varchar(256)     null comment 'url to the menu''s image file',
    sort_ord             int unsigned default 0 comment 'sort order',
    ins_id               varchar(32)  not null comment 'id of the account that created the menu',
    ins_time             datetime     not null comment 'datetime when the menu is created',
    status               varchar(3)   not null default '001' comment '000:created, 001:active, 002:inactive, 998:removed, 999:delete',
    primary key (site_id, menu_id)
) comment='site''s menu info';

drop table if exists crsnd_file;
create table crsnd_file (
    site_id              varchar(32)  not null comment 'site id',
    file_id              varchar(11)  not null comment 'file id',
    file_type            varchar(16)  not null default 'default' comment 'file type(application-defined)',
    file_name            varchar(128) not null comment 'file name',
    dir_id               varchar(5)   not null comment 'id of the directory or category',
    path                 varchar(128) not null comment 'path to the file',
    mime_type            varchar(128) default 'application/octet-stream' comment 'mime type of the file',
    file_size            bigint unsigned default 0 comment 'file size in byte',
    downloads            bigint unsigned default 0 comment 'download count',
    ins_id               varchar(32)  not null comment 'id of the account that created the file',
    ins_name             varchar(64)  not null comment 'name or alias of the account that created the file',
    ip_addr              varchar(18)  not null comment 'ip address of the account that created the file',
    ins_time             datetime     not null comment 'datetime when the file is created',
    status               varchar(3)   default '001' comment '001:active, 002:inactive, 998:removed, 999:delete',
    primary key (site_id, file_id),
    index (site_id, dir_id)
) comment='site''s file info';
/*
drop table if exists crsnd_category_attr;
create table crsnd_category_attr (
    site_id              varchar(32)  not null comment 'site id',
    category_id          varchar(8)   not null comment 'category id',
    attr_key             varchar(64)  not null comment 'attribute key',
    attr_val             text             null comment 'attribute value',
    primary key (site_id, category_id, attr_key)
) comment='category attributes extending the category''s fields';

drop table if exists crsnd_file_link;
create table crsnd_file_link (
    site_id              varchar(32)  not null comment 'site id',
    file_id              varchar(11)  not null comment 'file id',
    link_type            varchar(3)       null comment 'link type(application-defined)',
    link_target          varchar(16)  not null comment 'type of the link target(application-defined)',
    target_id            varchar(32)  not null comment 'target id',
    link_order           int unsigned default 0 comment 'link order',
    link_time            datetime     not null comment 'datetime when the link is created',
    primary key (site_id, file_id, link_type, link_target, target_id)
) comment='site''s file link info';
*/
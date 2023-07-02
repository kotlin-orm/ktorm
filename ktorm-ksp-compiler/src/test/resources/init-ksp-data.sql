create table "user"
(
    "id"       int          not null primary key auto_increment,
    "username" varchar(128) not null,
    "age"      int          not null,
    "gender"   int          not null default 0,
    "phone"    varchar(128) null
);


insert into "user"("username", "age", "gender")
values ('jack', 20, 0),
       ('lucy', 22, 1),
       ('mike', 22, 0);


create table "employee"
(
    "id"            int          not null primary key auto_increment,
    "name"          varchar(128) not null,
    "job"           varchar(128) not null,
    "manager_id"    int          null,
    "hire_date"     date         not null,
    "salary"        bigint       not null,
    "department_id" int          not null
);


insert into "employee"("name", "job", "manager_id", "hire_date", "salary", "department_id")
values ('vince', 'engineer', null, '2018-01-01', 100, 1),
       ('marry', 'trainee', 1, '2019-01-01', 50, 1),
       ('tom', 'director', null, '2018-01-01', 200, 2),
       ('penny', 'assistant', 3, '2019-01-01', 100, 2);

create table "province"
(
    "country"    varchar(128) not null,
    "province"   varchar(128) not null,
    "population" int          not null,
    primary key ("country", "province")
);

insert into "province"("country", "province", "population")
values ('China', 'Hebei', 130000),
       ('China', 'Henan', 140000);

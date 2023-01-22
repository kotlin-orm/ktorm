create extension if not exists hstore;
create extension if not exists earthdistance cascade;
create extension if not exists "uuid-ossp";

create table t_department(
  id serial primary key,
  name varchar(128) not null,
  location varchar(128) not null,
  "mixedCase" varchar(123)
);

create table t_employee(
  id serial primary key,
  name varchar(128) not null,
  job varchar(128) not null,
  manager_id int null,
  hire_date date not null,
  salary bigint not null,
  department_id int not null
);

create table t_multi_generated_key(
    id serial primary key,
    k varchar(128) not null default uuid_generate_v4(),
    v varchar(128)
);

create table t_metadata(
  id serial primary key,
  attrs hstore,
  numbers text[]
);

create type mood as enum ('SAD', 'HAPPY');
create table t_enum(
   id serial primary key,
   current_mood mood
);

create table t_json (obj json, arr json);

create table t_earthdistance(earth_field earth, cube_field cube);

create table t_user(
    id serial primary key,
    username varchar default 'default',
    age int
);

insert into t_department(name, location, "mixedCase") values ('tech', 'Guangzhou', 'one');
insert into t_department(name, location, "mixedCase") values ('finance', 'Beijing', 'two');

insert into t_employee(name, job, manager_id, hire_date, salary, department_id)
values ('vince', 'engineer', null, '2018-01-01', 100, 1);
insert into t_employee(name, job, manager_id, hire_date, salary, department_id)
values ('marry', 'trainee', 1, '2019-01-01', 50, 1);

insert into t_employee(name, job, manager_id, hire_date, salary, department_id)
values ('tom', 'director', null, '2018-01-01', 200, 2);
insert into t_employee(name, job, manager_id, hire_date, salary, department_id)
values ('penny', 'assistant', 3, '2019-01-01', 100, 2);

insert into t_metadata(attrs, numbers)
values ('a=>1, b=>2, c=>NULL'::hstore, array['a', 'b', 'c']);

insert into t_enum(current_mood)
values ('HAPPY')

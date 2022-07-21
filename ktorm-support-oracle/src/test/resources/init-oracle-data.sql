create table "t_department"(
  "id" int not null primary key,
  "name" varchar(128) not null,
  "location" varchar(128) not null,
  "mixedCase" varchar(128)
);

create table "t_employee"(
  "id" int not null primary key,
  "name" varchar(128) not null,
  "job" varchar(128) not null,
  "manager_id" int null,
  "hire_date" date not null,
  "salary" int not null,
  "department_id" int not null
);

insert into "t_department"("id", "name", "location") values (1, 'tech', 'Guangzhou');
insert into "t_department"("id", "name", "location") values (2, 'finance', 'Beijing');
insert into "t_department"("id", "name", "location", "mixedCase") values (3, 'ai', 'Hamburg', '123');

insert into "t_employee"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (1, 'vince', 'engineer', null, to_date('2018-01-01', 'yyyy-MM-dd'), 100, 1);
insert into "t_employee"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (2, 'marry', 'trainee', 1, to_date('2019-01-01', 'yyyy-MM-dd'), 50, 1);

insert into "t_employee"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (3, 'tom', 'director', null, to_date('2018-01-01', 'yyyy-MM-dd'), 200, 2);
insert into "t_employee"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (4, 'penny', 'assistant', 3, to_date('2019-01-01', 'yyyy-MM-dd'), 100, 2);



create table "t_employee0"(
  "id" int not null primary key,
  "name" varchar(128) not null,
  "job" varchar(128) not null,
  "manager_id" int null,
  "hire_date" date not null,
  "salary" int not null,
  "department_id" int not null
);

insert into "t_employee0"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (1, 'vince', 'engineer', null, to_date('2018-01-01', 'yyyy-MM-dd'), 100, 1);
insert into "t_employee0"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (2, 'marry', 'trainee', 1, to_date('2019-01-01', 'yyyy-MM-dd'), 50, 1);

insert into "t_employee0"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (3, 'tom', 'director', null, to_date('2018-01-01', 'yyyy-MM-dd'), 200, 2);
insert into "t_employee0"("id", "name", "job", "manager_id", "hire_date", "salary", "department_id")
    values (4, 'penny', 'assistant', 3, to_date('2019-01-01', 'yyyy-MM-dd'), 100, 2);

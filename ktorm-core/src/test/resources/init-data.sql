
create table t_department(
  id int not null primary key auto_increment,
  name varchar(128) not null,
  location varchar(128) not null
);

insert into t_department(name, location) values ('tech', 'Guangzhou');

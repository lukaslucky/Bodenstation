create table planeten
(
    name   text primary key,
    size_x int,
    size_y int
);

create table roboter
(
    name text primary key,
    planet text references planeten,
    position_x int,
    position_y int,
    direction text
);

create table felder
(
    planet text references planeten,
    x int,
    y int,
    ground text,
    primary key (planet,x,y)

);

create table temperatur
(
    temp int,
    roboter text references roboter,
    planet text,
    x int,
    y int,
    timestamp text default datetime(),
    foreign key (planet,x,y) references felder

);
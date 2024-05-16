# **Локальный поисковый движок по сайту**

![Screenshot 2024-05-16 175700](https://github.com/SlezaShaitana/10-4-chess-board/assets/132721004/1234658e-dc84-4ebb-8400-c1e17403c4d4)

## Описание

В этом проекте я разработала локальный поисковый движок для сайта, который помогает посетителям быстро находить нужную информацию. Веб-интерфейс состоит из трех вкладок: Dashboard, Management и Search.


![2024-05-16 18-00-17 - Trim](https://github.com/SlezaShaitana/search_engine/assets/132721004/50040eab-1e4e-44b4-8118-666b7be7fccf)



Поисковый движок представляет из себя веб-приложение, разработанное на основе фреймворка Spring Boot. Приложение работает с локально установленной базой данных MySQL, в которой хранится информация о всех страницах сайта и их содержимом.


При запуске приложения происходит индексация всех страниц сайта. Для этого используется библиотеку jsoup, которая позволяет парсить HTML-код страниц и извлекать из него нужную информацию. Полученные данные сохраняются в базу данных.

Пользователь вводит запрос в поле поиска, и приложение ищет в базе данных все страницы, содержащие все слова из запроса. Для учёта морфологии слов мы используем библиотеку morphology-russian. Полученные страницы ранжируются по релевантности и выводятся пользователю.


## Стек технологий
   
- Java

- Spring Boot

- MYSQL

- Lombok

- Maven

- Apache Lucene

- JPA (Hibernate)

- Jsoup

- REST

- JSON

- Slf4j

- HTML

- CSS

- JavaScript

- Thymeleaf

- jQuery


## В процессе разработки пришлось столкнуться с рядом проблем, таких как:

- Оптимизация скорости индексации и поиска;

- Зазработка алгоритма ранжирования страниц по релевантности;

- Учёт морфологии слов при поиске.

## Установка и настройка:

Убедитесь, что у вас установлены Java выше 11 версии, Maven и Git.
Склонируйте репозиторий проекта с GitHub:

```
git clone https://github.com/SlezaShaitana/search_engine.git
```

Cоздать локальную базу данных, и в файле application.properties указать параметры подключения к базе данных.

```
server:
 port: 8080

spring:
 datasource:
   username: user
   password: pass
   url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true
 jpa:
   properties:
     hibernate:
   dialect: org.hibernate.dialect.MySQL8Dialect
   hibernate:
     ddl-auto: update
   show-sql: true
```

Собрать проект с помощью сборщика maven

Открыть в браузере адрес http://localhost:8080/search-engine и начать использовать поисковый движок.

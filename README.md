# **Локальный поисковый движок по сайту**

![Screenshot 2024-05-16 175700](https://github.com/SlezaShaitana/10-4-chess-board/assets/132721004/1234658e-dc84-4ebb-8400-c1e17403c4d4)

## Описание

В этом проекте разработан локальный поисковый движок, который помогает посетителям быстро находить нужную информацию. Веб-интерфейс состоит из трех вкладок: Dashboard, Management и Search.

- Dashboard. Эта вкладка открывается по умолчанию. На ней отображается общая статистика по всем сайтам, а также детальная статистика и статус по каждому из сайтов.

  ![2024-05-16-18-00-17-1-Trim_1](https://github.com/SlezaShaitana/search_engine/assets/132721004/75e23fff-b673-416b-a1de-cce21034c724)

- Management. На этой вкладке находятся инструменты управления поисковым движком — запуск и остановка полной индексации (переиндексации), а также возможность добавить (обновить) отдельную страницу по ссылке.

![2024-05-16-18-00-17-3-Trim](https://github.com/SlezaShaitana/search_engine/assets/132721004/1f07fd7c-6dff-4dd3-ad64-e240dc49e802)

- Search. Эта страница предназначена для тестирования поискового движка. На ней находится поле поиска, выпадающий список с выбором сайта для поиска, а при нажатии на кнопку «Найти» выводятся результаты поиска.



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

  
## Техническое описание проекта:

Поисковый движок представляет из себя веб-приложение, разработанное на основе фреймворка Spring Boot. Приложение работает с локально установленной базой данных MySQL, в которой хранится информация о всех страницах сайта и их содержимом.

В конфигурационном файле перед запуском приложения задаются адреса сайтов, по которым движок должен осуществлять поиск.

При запуске приложения происходит индексация всех страниц сайта. Для этого используется библиотека jsoup, которая позволяет парсить HTML-код страниц и извлекать из него нужную информацию. Полученные данные сохраняются в базу данных.

Пользователь вводит запрос в поле поиска, и приложение ищет в базе данных все страницы, содержащие все слова из запроса. Для учёта морфологии слов используется библиотека morphology-russian. Полученные страницы ранжируются по релевантности и выводятся пользователю.

## В процессе разработки пришлось столкнуться с рядом проблем, таких как:

- Оптимизация скорости индексации и поиска;

- Зазработка алгоритма ранжирования страниц по релевантности;

- Учёт морфологии слов при поиске.
  

 ## Интерактивная документация

   ![image](https://github.com/SlezaShaitana/search_engine/assets/132721004/fde3be14-cb01-4291-b8fd-4a8593b42538)
    В рамках этого проекта создана интерактивная документация для API с помощью Swagger (OpenAPI Specification). Документация включает в себя полное описание всех путей, методов, параметров и ответов API, а также примеры запросов и ответов, которые помогают пользователям понять, как работает API.

  Интерактивная документация доступна по следующему адресу:

  ```
  http://localhost:8080/swagger-ui/index.html
  ```

## Установка и настройка:

Убедитесь, что у вас установлены следующие программы:

- Java Development Kit (JDK) версии 8 или выше
- Apache Maven версии 3.6.3 или выше
- MySQL Server версии 8.0 или выше
- GIT

Склонируйте репозиторий проекта с GitHub:

```
git clone https://github.com/SlezaShaitana/search_engine.git
```

Откройте терминал или командную строку и перейдите в каталог проекта:
```
cd search_engine
```

Cоздайте локальную базу данных, и в файле application.properties указать параметры подключения к базе данных.

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

Замените значения username, password и url на соответствующие значения для вашей базы данных.

Запустите проект, используя команду mvn:

```
mvn spring-boot:run
```
Эта команда запустит проект в режиме разработки, используя встроенный сервер Tomcat. Вы можете остановить проект, нажав Ctrl+C в терминале или командной строке.




Откройте веб-браузер и перейдите на страницу поискового движка

```
http://localhost:8080
```

Вы должны увидеть страницу поискового движка, на которой вы можете ввести запрос и получить результаты поиска.

Готово! Вы успешно запустили проект поискового движка на вашем локальном компьютере. 

[![Build status](https://ci.appveyor.com/api/projects/status/wywtrw4mvk7618pf?svg=true)](https://ci.appveyor.com/project/k-emiko/aqa3-2-ex2)


### Оговорки по запуску тестов
Данные тесты запускают SUT и базу данных при помощи testcontainers прямо из тестов, поэтому [docker-compose.yml](https://github.com/k-emiko/aqa3-2-ex1/blob/master/artifacts/docker-compose.yml) для их работы не требуется.
Однако условия задания требуют его предоставить, поэтому он есть в данном репозитории для демонстрации моего понимания того, как он должен быть настроен.
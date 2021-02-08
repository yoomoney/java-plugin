# java-plugin

## Описание

Плагин для сборки java проектов. Отвечает за следующий функционал:

- Инициализация wrapper'а для gradle
- Настройка компилятора Java
- Конфигурация сборки Jar-артефакта
- Настройка среды разработки
- Создание тестовых sourceSet и их настройка
- Подключение kotlin для тестов
- Проверка кода при помощи статического анализа
- Проверка покрытия кода тестами

## Настроки компиляции java кода

Плагин позволяет через gradle property `java-plugin.jvm-version` указать целевую версию java для проекта. 
Переопределить название свойства можно с помощью настройки: 
```groovy
    javaModule {
        jvmVersionPropertyName = "ru.yoomoney.gradle.plugin.java-plugin.jvm-version" //значение по умолчанию
    }
```
Данная версия будет использована для задания аргументов компиляции `--release`, `-source`, `-target`

Допускается указывать версию в двух форматах:
 - в legacy формате с двумя цифрами, например `1.8` 
 - в формате с одной мажорной версией, например `8`

## Статический анализ

Плагин проверяет, что количество предупреждений для заданного вида анализа не превышает значения,
заданного в файле static-analysis.properties.

Пример файла static-analysis.properties:

```properties
compiler=12
checkstyle=4
findbugs=2
```

Описание лимитов:

- `compiler` - количество предупреждений при компиляции. 
- `checkstyle` - количество проблем обнаруженных инструментом [Сheckstyle](https://github.com/checkstyle/checkstyle).
  Проверку можно отключить, добавив в build.gradle
```groovy
javaModule {
  checkstyleEnabled = false
}
```
- `findbugs` - количество проблем обнаруженных инструментом [Spotbugs](https://github.com/spotbugs/spotbugs).
  Проверку spotbugs можно отключить, добавив в build.gradle
```groovy
javaModule {
    spotbugsEnabled = false
}
```
## Проверка тестового покрытия

Плагин проверяет, что покрытие тестами не меньше лимитов (процент покрытия), заданных в файле coverage.properties.
Пример файла coverage.properties:

```properties
instruction=69
branch=28
method=76
class=80
```
Описание метрик покрытия, взято из https://www.eclemma.org/jacoco/trunk/doc/counters.html: 
* `instruction` - The smallest unit JaCoCo counts are single Java byte code instructions. Instruction coverage provides information about the amount of code that has been executed or missed. This metric is completely independent from source formatting and always available, even in absence of debug information in the class files.
* `branch` - JaCoCo also calculates branch coverage for all if and switch statements. This metric counts the total number of such branches in a method and determines the number of executed or missed branches. Branch coverage is always available, even in absence of debug information in the class files. Note that exception handling is not considered as branches in the context of this counter definition.
* `method` - Each non-abstract method contains at least one instruction. A method is considered as executed when at least one instruction has been executed. As JaCoCo works on byte code level also constructors and static initializers are counted as methods. Some of these methods may not have a direct correspondence in Java source code, like implicit and thus generated default constructors or initializers for constants.
* `class` - A class is considered as executed when at least one of its methods has been executed. Note that JaCoCo considers constructors as well as static initializers as methods. As Java interface types may contain static initializers such interfaces are also considered as executable classes.

## Настройка репозиториев

С помощью плагина возможно добавить репозитории, которые будут просмотрены при поиске зависимостей:
```groovy
javaModule {
    repositories = ["https://maven.java.net/content/repositories/public/"]
    snapshotsRepositories = ["https://maven.java.net/content/repositories/snapshots/"]
}
```
Репозитории из списка `snapshotsRepositories` будут добавлены только для фиче-веток.

## Настройка запуска тестов

Плагин автоматически конфигурирует TestNG для запуска тестов и предоставляет api для изменения некоторых настроек в каждом проекте:
```groovy
javaModule {
    test {
        listeners.add('my.custom.TestListener')
        threadCount = 2
    }
    componentTest {
        threadCount = 4
    }   
}
```

По умолчанию список listeners пуст, а threadCount равен 8.

**Не рекомендуется напрямую управлять настройками TestNg в проекте, иначе они неявно перезатрут ВСЕ настройки TestNG по умолчанию из плагина.**

## Подключение

Минимально работающая конфигурация:
```groovy
buildscript {
    repositories {
            //репозиторий, где хранится данный плагин
            maven { url "https://maven.java.net/content/repositories/public/" }
        }
    dependencies {
        classpath 'ru.yoomoney.gradle.plugins:java-plugin:1.+'
    }
}

apply plugin: 'ru.yoomoney.gradle.plugins.java-plugin'

```

## Kotlin

В состав проекта так же входит плагин для kotlin. 
Подходит **только** для проектов, где единственный язык написания кода - kotlin.

Плагин включает в себя функционал проверки кода при помощи линтера `ktlint` и статического анализатора `detekt`.
Количество допустимых предупреждений detekt задаётся в файле static-analysis.properties, название лимита - detekt.

Подключение плагина:
```groovy
buildscript {
    repositories {
        //репозиторий, где хранится данный плагин
        maven { url "https://maven.java.net/content/repositories/public/" }
    }
    dependencies {
        classpath 'ru.yoomoney.gradle.plugins:java-plugin:1.+'
    }
}

apply plugin: 'yamoney-kotlin-module-plugin'
```

# yamoney-java-module-plugin

## Описание

Плагин для сборки java модулей. Отвечает за следующий функционал:

- Настройка компилятора Java
- Конфигурация сборки Jar-артефакта
- Настройка yamoney-check-dependencies-plugin 
- Настройка среды разработки
- Создание тестовых sourceSet и их настройка
- Подключение kotlin для тестов
- Проверка кода при помощи статического анализа
- Проверка покрытия кода тестами

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


## Подключение
Минимально работающая конфигурация:
```groovy
buildscript {
    repositories {
        maven { url 'https://nexus.yamoney.ru/repository/gradle-plugins/' }
        maven { url 'https://nexus.yamoney.ru/repository/thirdparty/' }
        maven { url 'https://nexus.yamoney.ru/repository/central/' }
        maven { url 'https://nexus.yamoney.ru/repository/releases/' }
        maven { url 'https://nexus.yamoney.ru/repository/jcenter.bintray.com/' }
    }
    dependencies {
        classpath 'ru.yandex.money.gradle.plugins:yamoney-java-module-plugin:1.+'
    }
}

apply plugin: 'yamoney-java-module-plugin'

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
        maven { url 'https://nexus.yamoney.ru/repository/gradle-plugins/' }
        maven { url 'https://nexus.yamoney.ru/repository/thirdparty/' }
        maven { url 'https://nexus.yamoney.ru/repository/central/' }
        maven { url 'https://nexus.yamoney.ru/repository/releases/' }
        maven { url 'https://nexus.yamoney.ru/repository/jcenter.bintray.com/' }
    }
    dependencies {
        classpath 'ru.yandex.money.gradle.plugins:yamoney-java-module-plugin:1.+'
    }
}

apply plugin: 'yamoney-kotlin-module-plugin'
```

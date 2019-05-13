# yamoney-java-module-plugin

## Функционал плагина
- Плагин проверяет, что количество предупреждений при компиляции не превышает лимит. 
  Лимит должен быть задан в файле static-analysis.properties. Название настройки лимита - compiler
- Плагин проверяет, что покрытие тестами не меньше заданных лимитов.
  Лимиты задются в файле coverage.properties
- Плагин проверяет, что количесто предупреждений checkstyle не превышает лимит.
  Лимит должен быть задан в файле static-analysis.properties. Название настройки лимита - checkstyle.
  Проверку checkstyle можно отключить добавив в build.gradle
```groovy
javaModule {
  checkstyleEnabled = false
}
```
- Плагин проверяет, что количесто предупреждений spotbugs не превышает лимит.
  Лимит должен быть задан в файле static-analysis.properties. Название настройки лимита - findbugs.
  Проверку spotbugs можно отключить добавив в build.gradle
```groovy
javaModule {
    spotbugsEnabled = false
}
```
## Настройки
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

В состав проекта так же входит плагин для kotlin с подключением линтера и статического анализатора, походит только для проектов
где основной язык написания кода - kotlin.

Применение:
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

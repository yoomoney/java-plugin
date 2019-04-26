# yamoney-java-module-plugin

## Функционал плагина
- Плагин проверяет, что количество предупреждений при компиляции не превышает лимит. 
  Лимит должен быть задан в файле static-analysis.properties. Название настройки лимита - compiler

## Настройки
Минимально работающая конфигурация:
```groovy
buildscript {
    repositories {
        maven { url 'http://nexus.yamoney.ru/content/repositories/thirdparty/' }
        maven { url 'http://nexus.yamoney.ru/content/repositories/central/' }
        maven { url 'http://nexus.yamoney.ru/content/repositories/releases/' }
        maven { url 'http://nexus.yamoney.ru/content/repositories/jcenter.bintray.com/' }
    }
    dependencies {
        classpath 'ru.yandex.money.gradle.plugins:yamoney-java-module-plugin:1.+'
    }
}

apply plugin: 'yamoney-java-module-plugin'

```



- Example:

```
apply plugin: 'java'
apply plugin: 'groovy'
apply plugin: 'maven'

buildscript {
    repositories {
        mavenCentral()
        mavenLocal()

        maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
    }

    dependencies {
        classpath 'com.github.lburgazzoli:gradle-plugin-karaf-features-gen:1.0.0.SNAPSHOT'
    }
}

dependencies {
    compile "org.apache.geronimo.specs:geronimo-jpa_2.0_spec:1.1"
    compile "org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.1.1"
    compile "org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1"
    compile "org.apache.geronimo.specs:geronimo-j2ee-management_1.1_spec:1.0.1"
}


apply plugin: 'karaf-featuresgen'
```

- Run:


```
gradle generateKarafFeatures
```

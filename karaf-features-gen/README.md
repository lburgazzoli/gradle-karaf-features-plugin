
- Example:

```groovy
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

apply plugin: 'karaf-featuresgen'

karafFeatures {
    excludes = [
        'org.slf4j/.*',
        'log4j/.*',
        'org.osgi/.*',
        'org.apache.felix/.*',
        'org.apache.karaf.shell/.*'
    ]
}

```

- Run:

```
gradle generateKarafFeatures
```

- Single project output:

```xml
<features xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>
  <feature name='lb-karaf-common' version='1.0.0.SNAPSHOT'>
    <bundle>mvn:org.apache.commons/commons-lang3/3.1</bundle>
    <bundle>mvn:com.google.guava/guava/15.0</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jpa_2.0_spec/1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1</bundle>
  </feature>
</features>
```

- Multi project output:

```xml
<features xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>
  <feature name='jpa-batoojpa' version='1.0.0.SNAPSHOT'>
    <bundle>mvn:com.github.lburgazzoli/lb-karaf-common/1.0.0.SNAPSHOT</bundle>
    <bundle>mvn:org.apache.commons/commons-lang3/3.1</bundle>
    <bundle>mvn:commons-beanutils/commons-beanutils/1.8.3</bundle>
    <bundle>mvn:com.google.guava/guava/15.0</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jpa_2.0_spec/1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1</bundle>
    <bundle>mvn:com.github.lburgazzoli/jpa-common/1.0.0.SNAPSHOT</bundle>
    <bundle>mvn:org.batoo.jpa/batoo-jpa/2.0.1.3-SNAPSHOT</bundle>
  </feature>
  <feature name='jpa-common' version='1.0.0.SNAPSHOT'>
    <bundle>mvn:com.github.lburgazzoli/lb-karaf-common/1.0.0.SNAPSHOT</bundle>
    <bundle>mvn:org.apache.commons/commons-lang3/3.1</bundle>
    <bundle>mvn:commons-beanutils/commons-beanutils/1.8.3</bundle>
    <bundle>mvn:com.google.guava/guava/15.0</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jpa_2.0_spec/1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1</bundle>
    <bundle>mvn:org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1</bundle>
  </feature>
</features>
```

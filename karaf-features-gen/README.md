
- Example:

```groovy

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

apply plugin: 'java'
apply plugin: 'groovy'
apply plugin: 'maven'
apply plugin: 'karaf-featuresgen'


karafFeatures {
    excludes = [
        'org.slf4j/.*',
        'log4j/.*',
        'org.osgi/.*',
        'org.apache.felix/.*',
        'org.apache.karaf.shell/.*'
    ]

    wraps = [
        'com.google.guava/guava/.*'
    ]

    startLevels = [
        'org.apache.geronimo.specs/.*':'50',
        'org.apache.commons/.*':'60',
    ]
    
    // if outputFile is not provided, the output is written to the standard
    // output.
    outputFile = new File('features.xml')
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
    <bundle start-level='60'>mvn:org.apache.commons/commons-lang3/3.1</bundle>
    <bundle>wrap:mvn:com.google.guava/guava/15.0</bundle>
    <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jpa_2.0_spec/1.1</bundle>
    <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1</bundle>
    <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1</bundle>
    <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1</bundle>
  </feature>
</features>
```

- Multi project output:

```xml
<features xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>
  <features xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>
    <feature name='jpa-kundera-adapter' version='1.0.0.SNAPSHOT'>
      <bundle>mvn:com.github.lburgazzoli/lb-karaf-common/1.0.0.SNAPSHOT</bundle>
      <bundle start-level='60'>mvn:org.apache.commons/commons-lang3/3.1</bundle>
      <bundle>mvn:commons-beanutils/commons-beanutils/1.8.3</bundle>
      <bundle>wrap:mvn:com.google.guava/guava/15.0</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jpa_2.0_spec/1.1</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1</bundle>
      <bundle>mvn:com.impetus.core/kundera-core/2.7</bundle>
      <bundle>mvn:com.impetus.client/kundera-mongo/2.7</bundle>
      <bundle>mvn:org.mongodb/mongo-java-driver/2.11.3</bundle>
    </feature>
  </features>
  <features xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>
    <feature name='jpa-openjpa' version='1.0.0.SNAPSHOT'>
      <bundle>mvn:com.github.lburgazzoli/lb-karaf-common/1.0.0.SNAPSHOT</bundle>
      <bundle start-level='60'>mvn:org.apache.commons/commons-lang3/3.1</bundle>
      <bundle>mvn:commons-beanutils/commons-beanutils/1.8.3</bundle>
      <bundle>wrap:mvn:com.google.guava/guava/15.0</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jpa_2.0_spec/1.1</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jms_1.1_spec/1.1.1</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1</bundle>
      <bundle start-level='50'>mvn:org.apache.geronimo.specs/geronimo-j2ee-management_1.1_spec/1.0.1</bundle>
      <bundle>mvn:com.github.lburgazzoli/jpa-common/1.0.0.SNAPSHOT</bundle>
      <bundle>mvn:org.apache.openjpa/openjpa/2.2.2</bundle>
    </feature>
  </features>
</features>
```

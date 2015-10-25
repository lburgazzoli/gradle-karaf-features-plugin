gradle-karaf-features-plugin
============================

How to use
============================
build.gradle
```
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.github.lburgazzoli:gradle-karaf-features-plugin:2.7.0'
  }
}

group = 'com.github'
version = '1.0.0'
//name is 'project1'

project(':subproject1') {
  dependencies {
    compile 'commons-lang:commons-lang:2.6'
  }
}

project(':subproject2') {
  dependencies {
    compile 'commons-io:commons-io:2.4'
  }
}

karafFeatures {
  featuresName = 'featuresName'
  features {
    mainFeature {
      name = 'main-feature-name'
      repositories = ['mvn:group/dependent-feature/1.2.3/xml/features']
      description = 'Some useful description'
      dependencyFeatureNames = ['dependent-feature']
      project(':subproject1')
      project(':subproject2') {
        dependencies {
          transitive = false //true by default
        }
        artifactId = "newSubProject2" // project name by default
      }
    }
    testFeature {
      name = 'test-feature-name'
      description = 'Another useful description'
      dependencyFeatureNames = [karafFeatures.features.mainFeature.name]
    }
  }
}
```
  
To generate feature just run  
```
gradle generateKarafFeatures
```

generated file 'build/karafFeatures/project1-1.0.0-karaf.xml' will look like below  
```xml
<features xmlns='http://karaf.apache.org/xmlns/features/v1.2.0' name='featuresName'>
  <repository>mvn:group/dependent-feature/1.2.3/xml/features</repository>
  <feature name='main-feature-name' description='Some useful description' version='1.0.0'>
    <feature>dependent-feature</feature>
    <bundle>mvn:commons-lang/commons-lang/2.6</bundle>
    <bundle>mvn:com.github/subproject1/1.0.0</bundle>
    <bundle>mvn:com.github/newSubProject2/1.0.0</bundle>
  </feature>
  <feature name='test-feature-name' description='Another useful description' version='1.0.0'>
    <feature>main-feature-name</feature>
  </feature>
</features>
```

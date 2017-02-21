gradle-karaf-features-plugin
============================
[![Build Status](https://travis-ci.org/lburgazzoli/gradle-karaf-features-plugin.svg)](https://travis-ci.org/lburgazzoli/gradle-karaf-features-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.lburgazzoli/gradle-karaf-features-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.lburgazzoli/gradle-karaf-features-plugin)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

***This plugin is not more maintaned and its features have been incuded in https://github.com/lburgazzoli/gradle-karaf-plugin***

How to use
============================
build.gradle
```
// Gradle 2.1+
plugins {
  id "com.github.lburgazzoli.karaf.features" version "2.9.4"
}

group = 'com.github'
version = '1.0.0'
//name is 'project1'

configuration {
    myAdditionalDepList 
}

project(':subproject1') {
  dependencies {
    compile 'commons-lang:commons-lang:2.6'
    myAdditionalDepList 'com.google.guava:guava:18.0'        
    myAdditionalDepList "com.squareup.retrofit:retrofit:1.9.0"
    myAdditionalDepList "com.squareup.retrofit:converter-jackson:1.9.0"
  }
}

project(':subproject2') {
  dependencies {
    compile 'commons-io:commons-io:2.4'
  }
}

karafFeatures {
  name = 'featuresName'
  
  features {
    mainFeature {
      name = 'main-feature-name'      
      description = 'Some useful description'
      
      repository('mvn:group/dependent-feature/1.2.3/xml/features')
      feature('dependent-feature')
      
      
      project(':subproject1')
      project(':subproject2') {
        dependencies {
          transitive = false                  //true by default
        }
        artifactId = "newSubProject2"         // project name by default
      }
      
      bundlesFrom(project.configurations.myAdditionalDepList)
      bundle('com.squareup.retrofit:converter-jackson') {
        include = false
      }
    }
    testFeature {
      name = 'test-feature-name'
      description = 'Another useful description'
      feature(karafFeatures.features.mainFeature)
    }
  }
}
```
  
To generate feature just run  
```
gradle generateKarafFeatures
```

generated file `build/karafFeatures/project1-1.0.0-karaf.xml` will look like below  
```xml
<features xmlns='http://karaf.apache.org/xmlns/features/v1.2.0' name='featuresName'>
  <repository>mvn:group/dependent-feature/1.2.3/xml/features</repository>
  <feature name='main-feature-name' description='Some useful description' version='1.0.0'>
    <feature>dependent-feature</feature>
    <bundle>mvn:commons-lang/commons-lang/2.6</bundle>
    <bundle>wrap:mvn:com.squareup.retrofit/retrofit/1.9.0</bundle>
    <bundle>mvn:com.github/subproject1/1.0.0</bundle>
    <bundle>mvn:com.github/newSubProject2/1.0.0</bundle>
  </feature>
  <feature name='test-feature-name' description='Another useful description' version='1.0.0'>
    <feature>main-feature-name</feature>
  </feature>
</features>
```

Karaf 4 Support
============================
Karaf 4 features xsd v1.3.0 partially supported  
```xml
<feature version="1.2.3" dependency="true">dependent-feature</feature>
```

To generate this stuff  
1. Set xsdVersion to 1.3.0  
2. Use dependency with configuration closure  
```
karafFeatures {
  name = 'featuresName'
  xsdVersion = '1.3.0'
  outputFile = file("${project.buildDir}/karafFeatures/${project.name}-feature.xml")
  features {
    mainFeature {
      name = 'main-feature-name'
      feature('dependent-feature') {
        dependency = true              //false by default
        version = "1.2.3"              //empty by default
      }
    }
  }
}
```

generated file `build/karafFeatures/project1-feature.xml` will look like below  
```xml
<features xmlns='http://karaf.apache.org/xmlns/features/v1.3.0' name='featuresName'>
  <feature name='main-feature-name' version='1.0.0'>
    <feature version="1.2.3" dependency="true">dependent-feature</feature>
  </feature>
</features>
```

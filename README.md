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

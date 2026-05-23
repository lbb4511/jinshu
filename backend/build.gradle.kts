plugins {
    id("java")
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("jacoco")
}

allprojects {
    group = "com.jinshu"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.release.set(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }
}

// ── 根项目 JaCoCo 聚合报告 ──
tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    dependsOn(subprojects.map { it.tasks.withType<JacocoReport>() })

    val subprojectSourceSets = subprojects.map { it.sourceSets.main.get() }
    sourceDirectories.setFrom(subprojectSourceSets.map { it.allSource.srcDirs })
    classDirectories.setFrom(subprojectSourceSets.map { it.output })
    executionData.setFrom(subprojects.map {
        it.layout.buildDirectory.dir("jacoco/test.exec")
    })

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

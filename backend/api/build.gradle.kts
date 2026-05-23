plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
    }
    dependencies {
        dependency("org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1")
        dependency("org.projectlombok:lombok:1.18.46")
    }
}

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjrt")
    implementation("org.aspectj:aspectjweaver")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter")
    implementation("org.postgresql:postgresql")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.jinshu.api.ApiApplication")
}

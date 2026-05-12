plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.spring.boot.starter.batch)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.mybatis.spring.boot.starter)
    implementation(libs.postgresql)
    implementation(libs.easyexcel)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.jinshu.batch.BatchApplication")
}

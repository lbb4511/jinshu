plugins {
    id("java-library")
}

dependencies {
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.starter.aspectj)
    api(libs.spring.boot.starter.data.redis)
    api(libs.commons.lang3)
    api(libs.commons.io)
    api(libs.mybatis.spring.boot.starter)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

plugins {
    java
}

group = "com.extantape"
version = "0.8.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApi = "io.papermc.paper:paper-api:26.1.2.build.74-stable"

dependencies {
    compileOnly(paperApi)

    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<JavaExec>("pack") {
    description = "Builds the resource pack the release attaches."
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.extantape.paulbunyan.tools.PackBuilder")
    jvmArgs("-Djava.awt.headless=true")
    args(layout.buildDirectory.file("libs/PaulBunyan-pack-${project.version}.zip").get().asFile.path)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("PaulBunyan")
    from("LICENSE")
}

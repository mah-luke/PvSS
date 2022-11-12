plugins {
    id("java")
    id("application")
}

group = "org.fun"
version = "v0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_10
}

repositories {
    mavenCentral()

//    flatDir {
//        dirs 'libs'
//    }
}
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

//    compile 'at.ac.tuwien.ifs.sge:sge:1.0.2'
//    compile group: "at.ac.tuwien.ifs.sge", name: "sge", version: "1.0.2"

    implementation(fileTree("libs").matching {
        include("*exe.jar")
    })

    // manual selection (when using flatDir):
//    implementation('sge-1.0.2-exe')
//    implementation('sge-risk-1.0.2-exe')
}

tasks.withType<Jar>() {
    manifest {
        attributes["Main-Class"] = "org.fun.Main"
        attributes["Sge-Type"] = "agent"
        attributes["Agent-Class"] = "org.fun.SimpleFunAgent"
        attributes["Agent-Name"] = "FUNAgent"
    }
}

application {
    mainClass.set("org.fun.Main")
}


tasks.getByName<Test>("test"){
    useJUnitPlatform()
}

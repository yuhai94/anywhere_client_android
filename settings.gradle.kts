pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url=uri("https://maven.aliyun.com/repository/central")
        }
        google()
        mavenCentral()
        // 添加AWS Maven仓库
        maven {
            url = uri("https://aws.oss.sonatype.org/content/repositories/releases/")
        }
        maven {
            url=uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url=uri("https://maven.aliyun.com/repository/jcenter")
        }
    }
}

rootProject.name = "AwCli"
include(":app")

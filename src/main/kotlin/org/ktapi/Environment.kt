package org.ktapi

import ch.qos.logback.classic.util.ContextInitializer
import mu.KotlinLogging

/**
 * Initializes the environment.
 *
 * - The Environment name is set with a SystemProperty named 'environment' or an Environment Variable named
 * "ENVIRONMENT". If an environment isn't set, it will default to 'local'.
 *
 * - If a logback.xml file or logback-{environment}.xml file exists on the root of the classpath, then Logback
 * will be initialized using that file.
 *
 * - If an 'app-banner.txt' file exists on the root of the classpath it's contents will be printed out first to the
 * logger.
 *
 * - If a 'version' file exists on the root of the classpath it's contents will be set as the version on the
 * environment.
 *
 * - If the Environment variable 'IS_CI' is set to 'true', then isCI will be set to true. This is useful when you need
 * the system to behave differently during Continuous Integration tasks.
 */
object Environment : Init() {
    val name: String = System.getProperty("environment") ?: System.getenv("ENVIRONMENT") ?: "local"
    val version: String?
    val isLocal: Boolean
    val isProd: Boolean
    val isCI: Boolean

    init {
        System.setProperty("environment", name)

        val envLogback = "logback-$name.xml"
        val logbackConfig = if (envLogback.resourceExists()) envLogback else "logback.xml"
        if (logbackConfig.resourceExists()) {
            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, logbackConfig)
            System.setProperty("logging.config", "classpath:$logbackConfig")
        }

        val logger = KotlinLogging.logger {}

        val banner = "app-banner.txt".resourceAsString()
        if (banner != null) logger.info("\n$banner\n")

        logger.info("Environment: $name")

        version = "version".resourceAsString()
        if (version != null) logger.info("Version: $version")

        isLocal = name == "local"
        isProd = name == "prod"
        isCI = System.getenv("IS_CI") == "true"
    }

    val isNotLocal = !isLocal
    val isNotProd = !isProd
}

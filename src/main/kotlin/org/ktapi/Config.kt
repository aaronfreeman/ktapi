package org.ktapi

import mu.KotlinLogging
import org.yaml.snakeyaml.Yaml
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

inline fun <reified T : Any> config(key: String) = Config.value(key, T::class)
inline fun <reified T : Any> config(key: String, default: T) = configOrNull(key) ?: default
inline fun <reified T : Any> configList(key: String) = Config.valueList(key, T::class)
inline fun <reified T : Any> configList(key: String, default: List<T>) = configListOrNull(key) ?: default
inline fun <reified T : Any> configOrNull(key: String) = Config.valueOrNull(key, T::class)
inline fun <reified T : Any> configListOrNull(key: String) = Config.valueListOrNull(key, T::class)
fun configHasKey(key: String) = Config.hasKey(key)

inline fun <reified T : Any> lazyConfig(key: String): Lazy<T> = lazy { config(key) }
inline fun <reified T : Any> lazyConfig(key: String, default: T): Lazy<T> = lazy { config(key, default) }
inline fun <reified T : Any> lazyConfigOrNull(key: String): Lazy<T?> = lazy { configOrNull(key) }
inline fun <reified T : Any> lazyConfigList(key: String): Lazy<List<T>> = lazy { configList(key) }
inline fun <reified T : Any> lazyConfigListOrNull(key: String): Lazy<List<T>?> = lazy { configListOrNull(key) }
inline fun <reified T : Any> lazyConfigList(key: String, default: List<T>): Lazy<List<T>> =
    lazy { configList(key, default) }

/**
 * A wrapper around a list of ConfigSource objects that will be searched for config properties
 */
object Config : ConfigSource {
    private val logger = KotlinLogging.logger {}
    private val configs = mutableListOf<ConfigSource>()

    init {
        addSource(SystemPropertyConfig)
        addYaml("secret.yml")
        addYaml("app-${Environment.name}.yml")
        addYaml("app.yml")
    }

    private fun addYaml(path: String) {
        if (path.resourceExists()) {
            logger.info("Loading config from $path")
            addSource(YamlConfig(path))
        } else {
            logger.debug("No $path file found")
        }
    }

    fun addSource(config: ConfigSource) = configs.add(config)

    override fun valueOrNull(key: String) = configs.fold(null as String?) { value: String?, config: ConfigSource ->
        value ?: config.valueOrNull(key)
    }
}

interface ConfigSource {
    fun hasKey(key: String) = valueOrNull(key) != null

    fun <T : Any> value(name: String, type: KClass<T>) = valueOrNull(name, type) ?: error("Missing config value: $name")

    fun <T : Any> valueOrNull(key: String, type: KClass<T>): T? {
        var value = valueOrNull(key) ?: return null

        if (value.startsWith("$")) {
            value = System.getenv(value.substring(1)) ?: return null
        }

        return convertValue(value, type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> convertValue(value: String, type: KClass<T>) = when (type) {
        Boolean::class -> value.toBoolean() as T
        Double::class -> value.toDouble() as T
        Float::class -> value.toFloat() as T
        Int::class -> value.toInt() as T
        Long::class -> value.toLong() as T
        String::class -> value as T
        else -> {
            val instance = try {
                val clazz = Class.forName(value).kotlin
                clazz.objectInstance ?: clazz.createInstance()
            } catch (e: Exception) {
                null
            } ?: error("Unknown config type: ${type.java.name}")
            instance as T
        }
    }

    fun <T : Any> valueList(key: String, type: KClass<T>) =
        valueListOrNull(key, type) ?: error("Config key not found: $key")

    fun <T : Any> valueListOrNull(key: String, type: KClass<T>): List<T>? {
        val value = valueOrNull(key) ?: return null
        return convertToList(value.split("|"), type);
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> convertToList(valueList: List<String>, type: KClass<T>) =
        valueList.map { convertValue(it, type) }

    fun valueOrNull(key: String): String?
}

object SystemPropertyConfig : ConfigSource {
    override fun valueOrNull(key: String): String? = System.getProperty(key)
}

class YamlConfig(resourcePath: String) : ConfigSource {
    private val values = loadFromYaml(resourcePath)

    override fun valueOrNull(key: String) = values[key]

    @Suppress("UNCHECKED_CAST")
    fun loadFromYaml(path: String): Map<String, String> {
        val content = (Yaml().loadAll(path.resourceAsString()) as Iterable<LinkedHashMap<String, Any>>)
        return content.fold(mutableMapOf()) { values, map -> addValues(values, "", map) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addValues(
        values: MutableMap<String, String>,
        parent: String,
        map: Map<String, Any>
    ): MutableMap<String, String> {
        map.keys.forEach { key ->
            val fullKey = "$parent$key"
            when (val value = map[key]) {
                is LinkedHashMap<*, *> -> addValues(values, "$fullKey.", value as Map<String, Any>)
                is List<*> -> values[fullKey] = value.joinToString("|")
                else -> values[fullKey] = value.toString()
            }
        }
        return values
    }
}

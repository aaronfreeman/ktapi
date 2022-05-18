package org.ktapi

import java.net.URLEncoder

internal object Util {
    val classLoader: ClassLoader = Util.javaClass.classLoader
}

/**
 * This is a convenience class that adds an init function that can be called to
 * force the init block of the class to execute.
 *
 * @param blocks list of block of code that will be executed sequentially as part of the init
 */
open class Init(vararg blocks: () -> Any?) {
    init {
        blocks.forEach { it() }
    }

    /**
     * Initializes the object
     */
    fun init() = Unit
}


/**
 * @return the contents of the resource path in the string or null if it doesn't exist
 */
fun String.resourceAsString() = Util.classLoader.getResource(this)?.readText()

/**
 * @return true if the resource exists
 */
fun String.resourceExists() = Util.classLoader.getResource(this) != null

/**
 * @return the URL encoded string
 */
fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package app.revanced.patcher.patch

import kotlin.reflect.KProperty

class NoSuchOptionException(val option: String) : Exception("No such option: $option")
class IllegalValueException(val value: Any?) : Exception("Illegal value: $value")
class InvalidTypeException(val got: String, val expected: String) :
    Exception("Invalid option value type: $got, expected $expected")

class RequirementNotMetException : Exception("null was passed into an option that requires a value")

/**
 * A registry for an array of [PatchOption]s.
 * @param options An array of [PatchOption]s.
 */
class PatchOptions(vararg val options: PatchOption<*>) : Iterable<PatchOption<*>> {
    private val register = buildMap {
        for (option in options) {
            if (containsKey(option.key)) {
                throw IllegalStateException("Multiple options found with the same key")
            }
            put(option.key, option)
        }
    }

    /**
     * Get a [PatchOption] by its key.
     * @param key The key of the [PatchOption].
     */
    operator fun get(key: String) = register[key] ?: throw NoSuchOptionException(key)

    /**
     * Set the value of a [PatchOption].
     * @param key The key of the [PatchOption].
     * @param value The value you want it to be.
     * Please note that using the wrong value type results in a runtime error.
     */
    inline operator fun <reified T> set(key: String, value: T) {
        @Suppress("UNCHECKED_CAST") val opt = get(key) as PatchOption<T>
        if (opt.value !is T) throw InvalidTypeException(
            T::class.java.canonicalName,
            opt.value?.let { it::class.java.canonicalName } ?: "null"
        )
        opt.value = value
    }

    /**
     * Sets the value of a [PatchOption] to `null`.
     * @param key The key of the [PatchOption].
     */
    fun nullify(key: String) {
        get(key).value = null
    }

    override fun iterator() = options.iterator()
}

/**
 * A [Patch] option.
 * @param key Unique identifier of the option. Example: _`settings.microg.enabled`_
 * @param default The default value of the option.
 * @param title A human-readable title of the option. Example: _MicroG Settings_
 * @param description A human-readable description of the option. Example: _Settings integration for MicroG._
 * @param required Whether the option is required.
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class PatchOption<T>(
    val key: String,
    default: T?,
    val title: String,
    val description: String,
    val required: Boolean,
    val validator: (T?) -> Boolean
) {
    var value: T? = default
        set(value) {
            if (value == null && required) {
                throw RequirementNotMetException()
            }
            if (!validator(value)) {
                throw IllegalValueException(value)
            }
            field = value
        }

    /**
     * Gets the value of the option.
     * Please note that using the wrong value type results in a runtime error.
     */
    operator fun <T> getValue(thisRef: Nothing?, property: KProperty<*>) = value as T

    /**
     * Gets the value of the option.
     * Please note that using the wrong value type results in a runtime error.
     */
    inline operator fun <reified V> setValue(thisRef: Any?, property: KProperty<*>, new: V) {
        if (value !is V) throw InvalidTypeException(
            V::class.java.canonicalName,
            value?.let { it::class.java.canonicalName } ?: "null"
        )
        value = new as T
    }

    /**
     * A [PatchOption] representing a [String].
     * @see PatchOption
     */
    class StringOption(
        key: String,
        default: String?,
        title: String,
        description: String,
        required: Boolean = false,
        validator: (String?) -> Boolean = { true }
    ) : PatchOption<String>(
        key, default, title, description, required, validator
    )

    /**
     * A [PatchOption] representing a [Boolean].
     * @see PatchOption
     */
    class BooleanOption(
        key: String,
        default: Boolean?,
        title: String,
        description: String,
        required: Boolean = false,
        validator: (Boolean?) -> Boolean = { true }
    ) : PatchOption<Boolean>(
        key, default, title, description, required, validator
    )

    /**
     * A [PatchOption] with a list of allowed options.
     * @param options A list of allowed options for the [ListOption].
     * @see PatchOption
     */
    sealed class ListOption<E>(
        key: String,
        default: E?,
        val options: Iterable<E>,
        title: String,
        description: String,
        required: Boolean = false,
        validator: (E?) -> Boolean = { true }
    ) : PatchOption<E>(
        key, default, title, description, required, {
            (it?.let { it in options } ?: true) && validator(it)
        }
    ) {
        init {
            if (default !in options) {
                throw IllegalStateException("Default option must be an allowed options")
            }
        }
    }

    /**
     * A [ListOption] of type [String].
     * @see ListOption
     */
    class StringListOption(
        key: String,
        default: String?,
        options: Iterable<String>,
        title: String,
        description: String,
        required: Boolean = false,
        validator: (String?) -> Boolean = { true }
    ) : ListOption<String>(
        key, default, options, title, description, required, validator
    )

    /**
     * A [ListOption] of type [Int].
     * @see ListOption
     */
    class IntListOption(
        key: String,
        default: Int?,
        options: Iterable<Int>,
        title: String,
        description: String,
        required: Boolean = false,
        validator: (Int?) -> Boolean = { true }
    ) : ListOption<Int>(
        key, default, options, title, description, required, validator
    )
}
package org.danilopianini.upgradle

import java.util.concurrent.TimeUnit
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.AbstractDoubleTimeSource
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@ExperimentalTime
class CachedFor<T>(
    val duration: Duration,
    private val load: ()->T
): ReadOnlyProperty<Any?, T> {

    private val timeSource: TimeSource = TimeSource.Monotonic
    var lastTime = timeSource.markNow()
        private set
    var result: T = load()
        private set

    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if ((lastTime + duration).hasPassedNow()) {
            lastTime = timeSource.markNow()
            result = load()
        }
        return result
    }
}

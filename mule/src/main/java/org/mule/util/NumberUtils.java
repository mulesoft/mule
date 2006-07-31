package org.mule.util;


/**
 * <code>NumberUtils</code> contains useful methods for manipulating numbers.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class NumberUtils extends org.apache.commons.lang.math.NumberUtils
{
    public static long toLong(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Unable to convert null object to long");
        }
        else if (obj instanceof String) {
            return toLong((String) obj);
        }
        else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        else throw new IllegalArgumentException("Unable to convert object of type: " + obj.getClass().getName() + " to long.");
    }
}

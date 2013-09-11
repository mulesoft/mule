package org.mule.util;

/**
 * <p>
 * Utility class to validate Preconditions
 * </p>
 */
public class Preconditions
{

    /**
     * @param condition Condition that the argument must satisfy
     * @param message   The Message of the exception in case the condition is invalid
     */
    public static void checkArgument(boolean condition, String message)
    {
        if (!condition)
        {
            throw new IllegalArgumentException(message);
        }
    }
}

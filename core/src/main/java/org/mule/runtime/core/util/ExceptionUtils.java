/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import org.mule.runtime.api.connection.ConnectionException;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

/**
 * Mule exception utilities.
 */
public class ExceptionUtils extends org.apache.commons.lang.exception.ExceptionUtils
{
    /**
     * This method returns true if the throwable contains a {@link Throwable} that
     * matches the specified class or subclass in the exception chain. Subclasses of
     * the specified class do match.
     *
     * @param throwable the throwable to inspect, may be null
     * @param type the type to search for, subclasses match, null returns false
     * @return the index into the throwable chain, false if no match or null input
     */
    public static boolean containsType(Throwable throwable, Class<?> type)
    {
        return indexOfType(throwable, type) > -1;
    }

    /**
     * This method returns the throwable closest to the root cause that matches the
     * specified class or subclass. Any null argument will make the method return
     * null.
     *
     * @param throwable the throwable to inspect, may be null
     * @param type the type to search for, subclasses match, null returns null
     * @return the throwable that is closest to the root in the throwable chain that
     *         matches the type or subclass of that type.
     */
    @SuppressWarnings("unchecked")
    public static <ET> ET getDeepestOccurenceOfType(Throwable throwable, Class<ET> type)
    {
        if (throwable == null || type == null)
        {
            return null;
        }
        List<Throwable> throwableList = getThrowableList(throwable);
        ListIterator<Throwable> listIterator = throwableList.listIterator(throwableList.size());
        while (listIterator.hasPrevious())
        {
            Throwable candidate = listIterator.previous();
            if (type.isAssignableFrom(candidate.getClass()))
            {
                return (ET)candidate;
            }
        }
        return null;
    }

    /**
     * Similar to {@link #getFullStackTrace(Throwable)} but removing the exception and causes
     * messages. This is useful to determine if two exceptions have matching stack traces regardless of
     * the messages which may contain invokation specific data
     *
     * @param throwable the throwable to inspect, may be <code>null</code>
     * @return the stack trace as a string, with the messages stripped out. Empty string if throwable was <code>null</code>
     */
    public static String getFullStackTraceWithoutMessages(Throwable throwable)
    {
        StringBuilder builder = new StringBuilder();

        for (String frame : getStackFrames(throwable))
        {
            builder.append(frame.replaceAll(":\\s+([\\w\\s]*.*)", "").trim()).append(LINE_SEPARATOR);
        }

        return builder.toString();
    }



    /**
     * Introspects the {@link Throwable} to obtain the first
     * Connection Exception cause if exists.
     *
     * This method handles recursive cause structures
     * that might otherwise cause infinite loops. If the throwable parameter
     * is a {@link ConnectionException} the same value will be returned.
     * If the throwable parameter has a cause of itself,
     * then an empty value will be returned.
     *
     * @param throwable the throwable to get the root cause for
     * @return an Optional {@link ConnectionException} cause of the <code>Throwable</code>
     */
    @SuppressWarnings("unchecked")
    public static Optional<ConnectionException> extractRootConnectionException(Throwable throwable)
    {
        if(throwable != null)
        {
            if (isConnectionException(throwable))
            {
                return Optional.of((ConnectionException) throwable);
            }
            Optional<? extends Throwable> connectionException = Arrays.stream(getThrowables(throwable)).filter(ExceptionUtils::isConnectionException).findFirst();
            return (Optional<ConnectionException>) connectionException;
        }
        return Optional.empty();
    }

    private static boolean isConnectionException(Throwable throwable)
    {
        return throwable instanceof ConnectionException;
    }

}

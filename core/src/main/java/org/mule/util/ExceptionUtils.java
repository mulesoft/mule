/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.util.List;
import java.util.ListIterator;

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
     * @return the throwablethat is closest to the root in the throwable chain that
     *         matches the type or subclass of that type.
     */
    public static Throwable getDeepestOccurenceOfType(Throwable throwable, Class<?> type)
    {
        if (throwable == null || type == null)
        {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Throwable> throwableList = getThrowableList(throwable);
        ListIterator<Throwable> listIterator = throwableList.listIterator(throwableList.size());
        while (listIterator.hasPrevious())
        {
            Throwable candidate = listIterator.previous();
            if (type.isAssignableFrom(candidate.getClass()))
            {
                return candidate;
            }
        }
        return null;
    }
}

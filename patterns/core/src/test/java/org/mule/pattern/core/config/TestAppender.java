/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.pattern.core.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.NullAppender;

/**
 * Allows to check log events occurrences in a test case.
 */
public class TestAppender extends NullAppender
{

    private static ThreadLocal<Set<Expectation>> expectations = new ThreadLocal<>();

    private static Set<Expectation> expectationsInstance()
    {
        if (expectations.get() == null)
        {
            expectations.set(new HashSet<Expectation>());
        }
        return expectations.get();
    }

    public static void clear()
    {
        expectationsInstance().clear();
    }

    public static void ensure(Expectation ... expectationsToCheck)
    {
        Set s = new HashSet();
        s.addAll(Arrays.asList(expectationsToCheck));
        ensure(s);
    }

    public static void ensure(Set<Expectation> expectationsToCheck)
    {
        if (!expectationsInstance().equals(expectationsToCheck))
        {
            throw new RuntimeException(difference(expectationsToCheck, expectationsInstance()));
        }
    }

    private static String difference(Set<Expectation> expected, Set<Expectation> actual)
    {
        StringBuilder builder = new StringBuilder();
        addCollection(builder, CollectionUtils.subtract(actual, expected), "Not expected but received:");
        addCollection(builder, CollectionUtils.subtract(expected, actual), "Expected but not received:");
        return builder.toString();
    }

    private static void addCollection(StringBuilder builder, Collection items, String description)
    {
        if(items!=null && !items.isEmpty())
        {
            builder.append('\n').append(description);
            for(Object item: items)
            {
                builder.append('\n').append(item);
            }
        }
    }

    @Override
    public void doAppend(LoggingEvent event)
    {
        expectationsInstance().add(new Expectation(event.getLevel().toString(), event.getLoggerName(), event.getRenderedMessage()));
    }

    public static class Expectation
    {

        private String level;
        private String category;
        private String message;

        public Expectation(String level, String category, String message)
        {
            this.level = level;
            this.category = category;
            this.message = message;
        }

        @Override
        public String toString()
        {
            return "Expectation{" +
                   "level='" + level + '\'' +
                   ", category='" + category + '\'' +
                   ", message='" + message + '\'' +
                   '}';
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            Expectation that = (Expectation) o;

            if (category != null ? !category.equals(that.category) : that.category != null)
            {
                return false;
            }
            if (level != null ? !level.equals(that.level) : that.level != null)
            {
                return false;
            }
            if (message != null ? !message.equals(that.message) : that.message != null)
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = level != null ? level.hashCode() : 0;
            result = 31 * result + (category != null ? category.hashCode() : 0);
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
        }
    }

}


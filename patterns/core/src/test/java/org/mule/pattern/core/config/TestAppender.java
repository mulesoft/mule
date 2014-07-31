/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.pattern.core.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * Allows to check log events occurrences in a test case.
 */
public class TestAppender extends AbstractAppender
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

    public static void ensure(Expectation... expectationsToCheck)
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
        if (items != null && !items.isEmpty())
        {
            builder.append('\n').append(description);
            for (Object item : items)
            {
                builder.append('\n').append(item);
            }
        }
    }

    public TestAppender(String name, Filter filter, Layout<? extends Serializable> layout)
    {
        super(name, filter, layout);
    }

    public TestAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event)
    {
        expectationsInstance().add(new Expectation(event.getLevel().toString(), event.getLoggerName(), event.getMessage().getFormattedMessage()));
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
            return String.format("Expectation {level='%s', category='%s', message='%s'}", level, category, message);
        }

        @Override
        public boolean equals(Object other)
        {
            return EqualsBuilder.reflectionEquals(this, other);
        }

        @Override
        public int hashCode()
        {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }
}


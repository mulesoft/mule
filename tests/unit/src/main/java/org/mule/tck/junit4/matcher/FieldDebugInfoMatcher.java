/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.debug.SimpleFieldDebugInfo;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class FieldDebugInfoMatcher extends TypeSafeMatcher<FieldDebugInfo>
{

    private final String name;
    private final Class type;
    private final Matcher matcher;

    public FieldDebugInfoMatcher(String name, Class type, Object value)
    {
        this.name = name;
        this.type = type;
        this.matcher = equalTo(value);
    }

    public FieldDebugInfoMatcher(String name, Class type, Matcher matcher)
    {
        this.name = name;
        this.type = type;
        this.matcher = matcher;
    }

    @Override
    public boolean matchesSafely(FieldDebugInfo item)
    {
        boolean sameValue = matcher.matches(item.getValue());

        return name.equals(item.getName()) && sameValue && type == item.getType();
    }

    public void describeTo(Description description)
    {
        description.appendText(format("a %s with name: '%s' type: '%s' and value that is ", SimpleFieldDebugInfo.class.getSimpleName(), name, type));
        matcher.describeTo(description);
    }

    @Factory
    public static Matcher<FieldDebugInfo> fieldLike(String name, Class type, Object value)
    {
        return new FieldDebugInfoMatcher(name, type, value);
    }

    @Factory
    public static Matcher<FieldDebugInfo> fieldLike(String name, Class type, Matcher matcher)
    {
        if (matcher == null)
        {
            throw new IllegalArgumentException("Matcher cannot be null");
        }

        return new FieldDebugInfoMatcher(name, type, matcher);
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import static java.lang.String.format;
import org.mule.api.debug.FieldDebugInfo;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class FieldDebugInfoMatcher extends TypeSafeMatcher<FieldDebugInfo>
{

    private final String name;
    private final Class type;
    private final Object value;

    public FieldDebugInfoMatcher(String name, Class type, Object value)
    {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean matchesSafely(FieldDebugInfo item)
    {
        boolean sameValue = value != null && value.equals(item.getValue()) || value == null && item.getValue() == null;

        return name.equals(item.getName()) && sameValue && type == item.getType();
    }

    public void describeTo(Description description)
    {
        description.appendText(format("a field debug info with name: '%s' type: '%s' value: '%s'", name, type, value));
    }

    @Factory
    public static Matcher<FieldDebugInfo> fieldLike(String name, Class type, Object value)
    {
        return new FieldDebugInfoMatcher(name, type, value);
    }
}

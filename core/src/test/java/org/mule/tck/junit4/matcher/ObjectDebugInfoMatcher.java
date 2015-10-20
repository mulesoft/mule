/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.debug.ObjectFieldDebugInfo;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ObjectDebugInfoMatcher extends TypeSafeMatcher<FieldDebugInfo>
{

    private final String name;
    private final Class type;
    private final List<Matcher<FieldDebugInfo>> fieldMatchers;

    public ObjectDebugInfoMatcher(String name, Class type, List<Matcher<FieldDebugInfo>> fieldMatchers)
    {
        this.name = name;
        this.type = type;
        this.fieldMatchers = fieldMatchers;
    }

    @Override
    protected boolean matchesSafely(FieldDebugInfo fieldDebugInfo)
    {
        if (!name.equals(fieldDebugInfo.getName()))
        {
            return false;
        }

        if (!(fieldDebugInfo instanceof ObjectFieldDebugInfo))
        {
            return false;
        }

        if (type != null && !type.equals(fieldDebugInfo.getType()) || type == null && fieldDebugInfo.getType() != null)
        {
             return false;
        }

        final ObjectFieldDebugInfo objectFieldDebugInfo = (ObjectFieldDebugInfo) fieldDebugInfo;
        final List<FieldDebugInfo> fieldDebugInfos = objectFieldDebugInfo.getValue();

        if (fieldMatchers.size() != fieldDebugInfos.size())
        {
            return false;
        }

        try
        {
            for (Matcher<FieldDebugInfo> fieldMatcher : fieldMatchers)
            {
                assertThat(fieldDebugInfos, hasItem(fieldMatcher));
            }
            return true;
        }
        catch (AssertionError e)
        {
            // Ignore
        }

        return false;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText(format("an %s with name: '%s' type: '%s' and containing [ ", ObjectFieldDebugInfo.class.getSimpleName(), name, type));
        boolean firstMatcher = true;
        for (Matcher<FieldDebugInfo> fieldMatcher : fieldMatchers)
        {
            if (firstMatcher)
            {
                firstMatcher = false;
            }
            else
            {
                description.appendText(", ");
            }
            fieldMatcher.describeTo(description);
        }
        description.appendText("]");
    }

    @Factory
    public static Matcher<FieldDebugInfo> objectLike(String name, Class type, List<Matcher<FieldDebugInfo>> fieldMatchers)
    {
        return new ObjectDebugInfoMatcher(name, type, fieldMatchers);
    }
}

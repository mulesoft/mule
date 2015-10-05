/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.debug.ObjectFieldDebugInfo;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

public class ObjectDebugInfoMatcher extends TypeSafeMatcher<FieldDebugInfo>
{

    private final String name;
    private final Class type;
    private final List<FieldDebugInfo> fields;

    public ObjectDebugInfoMatcher(String name, Class type, List<FieldDebugInfo> fields)
    {
        this.name = name;
        this.type = type;
        this.fields = fields;
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

        if (fields.size() != fieldDebugInfos.size())
        {
            return false;
        }

        try
        {
            for (FieldDebugInfo field : fields)
            {
                MatcherAssert.assertThat(fieldDebugInfos, Matchers.hasItem(FieldDebugInfoMatcher.fieldLike(field.getName(), field.getType(), field.getValue())));

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
        description.appendText(String.format("an object debug info with name: '%s' type: '%s' fields: '%s'", name, type, fields));
    }

    @Factory
    public static Matcher<FieldDebugInfo> objectLike(String name, Class type, List<FieldDebugInfo> fields)
    {
        return new ObjectDebugInfoMatcher(name, type, fields);
    }
}

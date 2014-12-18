/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import static org.junit.Assert.assertThat;
import org.mule.module.http.internal.ParameterMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;

public class ParamMapMatcher
{

    public static Matcher<ParameterMap> isEqual(final Map<String, Collection<Object>> parameters)
    {
        return new BaseMatcher<ParameterMap>()
        {
            @Override
            public boolean matches(Object o)
            {
                ParameterMap parameterMap = (ParameterMap) o;
                assertThat(parameterMap.size(), Is.is(parameters.size()));
                for (String key : parameters.keySet())
                {
                    assertThat(parameterMap.keySet(), Matchers.containsInAnyOrder(parameters.keySet().toArray(new String[parameterMap.size()])));
                    final Collection<Object> parameterKeyValues = parameters.get(key);
                    final List<String> parameterMapValues = parameterMap.getAsList(key);
                    assertThat(parameterMapValues, Matchers.containsInAnyOrder(parameterKeyValues.toArray(new String[parameterKeyValues.size()])));
                }
                return true;
            }

            @Override
            public void describeTo(Description description)
            {
            }
        };
    }

}

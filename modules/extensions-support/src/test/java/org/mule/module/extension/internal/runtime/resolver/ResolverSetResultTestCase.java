/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.module.extension.HealthStatus;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ResolverSetResultTestCase extends AbstractMuleTestCase
{

    private static final String NAME = "MG";
    private static final int AGE = 31;
    private static final HealthStatus HEALTH = null;

    private ParameterModel nameParameterModel;
    private ParameterModel ageParameterModel;
    private ParameterModel healthParameterModel;

    private ResolverSetResult result;

    @Before
    public void before() throws Exception
    {
        nameParameterModel = getParameter("myName", String.class);
        ageParameterModel = getParameter("age", Integer.class);
        healthParameterModel = getParameter("initialHealth", HealthStatus.class);

        result = buildResult();
    }

    @Test
    public void testValues()
    {
        assertResult(result);
    }

    @Test
    public void equals()
    {
        assertThat(result, equalTo(buildResult()));
    }

    @Test
    public void notEquals()
    {
        assertThat(result, not(equalTo(agelessResult())));
    }

    @Test
    public void equivalentHashCode()
    {
        assertThat(result.hashCode(), equalTo(buildResult().hashCode()));
    }

    @Test
    public void nonEquivalentHashCode()
    {
        assertThat(result.hashCode(), not(equalTo(agelessResult().hashCode())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullParameter()
    {
        getBuilder().add(null, "blah");
    }

    @Test(expected = NoSuchElementException.class)
    public void invalidParameter()
    {
        result.get(getParameter("invalid", String.class));
    }

    private void assertResult(ResolverSetResult result)
    {
        assertThat((String) result.get(nameParameterModel), is(NAME));
        assertThat((Integer) result.get(ageParameterModel), is(AGE));
        assertThat((HealthStatus) result.get(healthParameterModel), is(HEALTH));
    }

    private ResolverSetResult buildResult()
    {
        return getBuilder().build();
    }

    private ResolverSetResult agelessResult()
    {
        return getBuilder()
                .add(ageParameterModel, null)
                .build();
    }

    private ResolverSetResult.Builder getBuilder()
    {
        return ResolverSetResult.newBuilder()
                .add(nameParameterModel, NAME)
                .add(ageParameterModel, AGE)
                .add(healthParameterModel, HEALTH);
    }
}

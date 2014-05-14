/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.extensions.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.HealthStatus;
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

    private Parameter nameParameter;
    private Parameter ageParameter;
    private Parameter healthParameter;

    private ResolverSetResult result;

    @Before
    public void before() throws Exception
    {
        nameParameter = getParameter("myName", String.class);
        ageParameter = getParameter("age", Integer.class);
        healthParameter = getParameter("initialHealth", HealthStatus.class);

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
        assertThat((String) result.get(nameParameter), is(NAME));
        assertThat((Integer) result.get(ageParameter), is(AGE));
        assertThat((HealthStatus) result.get(healthParameter), is(HEALTH));
    }

    private ResolverSetResult buildResult()
    {
        return getBuilder().build();
    }

    private ResolverSetResult agelessResult()
    {
        return getBuilder()
                .add(ageParameter, null)
                .build();
    }

    private ResolverSetResult.Builder getBuilder()
    {
        return ResolverSetResult.newBuilder()
                .add(nameParameter, NAME)
                .add(ageParameter, AGE)
                .add(healthParameter, HEALTH);
    }
}

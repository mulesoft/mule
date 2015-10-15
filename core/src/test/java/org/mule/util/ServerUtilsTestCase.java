/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.hamcrest.core.Is;
import org.junit.Test;

import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.util.ServerUtils.getMuleBase;
import static org.mule.util.ServerUtils.getMuleHome;

@SmallTest
public class ServerUtilsTestCase extends AbstractMuleTestCase
{

    private static final String EXPECTED_MULE_HOME_VALUE = "expected-mule-home-value";
    private static final String EXPECTED_MULE_BASE_VALUE = "expected-mule-base-value";

    @Test
    public void muleHome() throws Exception
    {
        testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, EXPECTED_MULE_HOME_VALUE, new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                assertThat(getMuleHome(), Is.is(EXPECTED_MULE_HOME_VALUE));
            }
        });
    }

    @Test
    public void muleHomeIsNullWhenNotDefined() throws Exception
    {
        assertThat(getMuleHome(), nullValue());
    }

    @Test
    public void muleBase() throws Exception
    {
        testWithSystemProperty(MULE_BASE_DIRECTORY_PROPERTY, EXPECTED_MULE_BASE_VALUE, new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                assertThat(getMuleBase(), Is.is(EXPECTED_MULE_BASE_VALUE));
            }
        });
    }

    @Test
    public void muleBaseReturnsMuleHomeWhenNotSet() throws Exception
    {
        testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, EXPECTED_MULE_HOME_VALUE, new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                assertThat(getMuleBase(), Is.is(EXPECTED_MULE_HOME_VALUE));
            }
        });
    }

    @Test
    public void muleBaseReturnsNullIfNetherMuleHomeOrMuleBaseIsSet() throws Exception
    {
        assertThat(getMuleBase(), nullValue());
    }
}
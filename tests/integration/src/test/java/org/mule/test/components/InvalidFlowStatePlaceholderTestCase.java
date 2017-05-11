/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InvalidFlowStatePlaceholderTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private MuleContext context;

    @Before
    public void before() throws InitialisationException, ConfigurationException
    {
        context = new DefaultMuleContextFactory().createMuleContext();
    }

    @After
    public void after()
    {
        if (context != null)
        {
            context.dispose();
        }
    }

    @Test
    public void emptyVariableNameValidatedBySchema() throws Exception
    {
        thrown.expect(ConfigurationException.class);
        thrown.expect(hasMessage(containsString("'${state}]' is not a valid value of union type '#AnonType_initialStateflowType'")));

        SpringXmlConfigurationBuilder builder =
                new SpringXmlConfigurationBuilder("org/mule/test/components/invalid-flow-initial-state.xml");
        builder.configure(context);
    }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionalObjectsIgnoredTestCase extends AbstractMuleTestCase
{

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionalObjectsIgnoredTestCase.class);
    private static final String OPTIONAL_OBJECT_KEY = "problematic";

    private MuleContext muleContext;

    @Before
    public void before() throws Exception
    {
        muleContext = new DefaultMuleContextFactory().createMuleContext(new SpringXmlConfigurationBuilder(new String[] {}));
        muleContext.start();
        muleContext.getRegistry().lookupByType(Calendar.class);
    }

    @After
    public void after() throws Exception
    {
        if (muleContext != null)
        {
            LifecycleUtils.disposeIfNeeded(muleContext, LOGGER);
        }
    }

    @Test
    public void optionalObjectDetected() throws Exception
    {
        SpringRegistryBootstrap registryBootstrap = muleContext.getRegistry().lookupObject(SpringRegistryBootstrap.class);
        assertThat(registryBootstrap, is(not(nullValue())));
        OptionalObjectsController optionalObjectsController = registryBootstrap.getOptionalObjectsController();
        assertThat(optionalObjectsController.isOptional(OPTIONAL_OBJECT_KEY), is(true));
        assertThat(optionalObjectsController.isDiscarded(OPTIONAL_OBJECT_KEY), is(true));
    }

    @Test
    public void optionalObjectSafelyIgnored()
    {
        assertThat(muleContext.getRegistry().lookupObject(OPTIONAL_OBJECT_KEY), is(nullValue()));
    }
}

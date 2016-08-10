/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
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
        muleContext = new DefaultMuleContextFactory().createMuleContext(new SpringXmlConfigurationBuilder(new String[] {}, emptyMap(), APP));
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
    public void optionalObjectSafelyIgnored()
    {
        assertThat(muleContext.getRegistry().lookupObject(OPTIONAL_OBJECT_KEY), is(nullValue()));
    }
}

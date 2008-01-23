/*
 * $Id: CoreNamespaceRoutersTestCase.java 10118 2007-12-19 20:44:47Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

import junit.framework.TestCase;

public class EmbeddedMuleTestCase extends TestCase
{
    public void testStartup() throws Exception
    {
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(
            "org/mule/test/spring/mule-root-test.xml");
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        MuleContext context = muleContextFactory.createMuleContext(builder);
        // MuleContext must be started explicitly after MULE-1988
        assertFalse(context.isStarted());
        context.start();
        assertTrue(context.isStarted());
    }
}

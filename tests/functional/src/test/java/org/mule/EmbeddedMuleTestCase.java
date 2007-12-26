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

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.UMOManagementContext;

import junit.framework.TestCase;

public class EmbeddedMuleTestCase extends TestCase
{
    public void testStartup() throws Exception
    {
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        UMOManagementContext context = builder.configure("org/mule/test/spring/mule-root-test.xml");
        assertFalse(context.isStarted());
        context.start();
        assertTrue(context.isStarted());
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleContext;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.ClassUtils;

public class MuleServerTestCase extends AbstractMuleTestCase
{

    // @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    public void testMuleServer() throws Exception
    {
        MuleServer muleServer = new MuleServer();
        assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(),
            muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEV_MODE_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    public void testMuleServerResource() throws Exception
    {
        MuleServer muleServer = new MuleServer("org/mule/test/spring/config1/test-xml-mule2-config.xml");
        assertEquals("org/mule/test/spring/config1/test-xml-mule2-config.xml", muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEV_MODE_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    public void testMuleServerConfigArg() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-config",
            "org/mule/test/spring/config1/test-xml-mule2-config.xml"});
        assertEquals("org/mule/test/spring/config1/test-xml-mule2-config.xml", muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEV_MODE_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    public void testMuleServerMultipleSpringConfigArgs() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-config",
            "mule-config.xml,org/mule/test/spring/config1/test-xml-mule2-config.xml"});
        assertEquals("mule-config.xml,org/mule/test/spring/config1/test-xml-mule2-config.xml",
            muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEV_MODE_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    public void testMuleServerBuilerArg() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-builder",
            "org.mule.config.spring.SpringXmlConfigurationBuilder"});
        assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(),
            muleServer.getConfigurationResources());
        assertEquals("org.mule.config.spring.SpringXmlConfigurationBuilder", MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    public void testMuleServerSpringBuilerArg() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-builder", "spring"});
        assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(),
            muleServer.getConfigurationResources());
        assertEquals("org.mule.config.spring.SpringXmlConfigurationBuilder", MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

}

/*
 * $Id:MuleBeanDefinitionReaderTestCase.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.routing.UMORouterCatchAllStrategy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

public class MuleBeanDefinitionReaderTestCase extends AbstractConfigBuilderTestCase
{

    public String getConfigResources()
    {
        // A Mule Xml config file and a Spring context file
        return "test-xml-mule-config-split-with-beans.xml," +
                "test-xml-mule-config.xml," +
                "test-application-context.xml," +
                "test-xml-mule-config-split.xml," +
                "test-mule-to-spring-with-xslt.xml";
    }

    public ConfigurationBuilder getBuilder()
    {
        return new MuleXmlConfigurationBuilder();
    }

    // Test spring bean configs
    public void testTransformerBean()
    {
        TestCompressionTransformer c = (TestCompressionTransformer) AbstractMuleTestCase.managementContext.getRegistry().lookupTransformer("beanTransformer");
        Assert.assertNotNull(c);
        Assert.assertEquals("hello",c.getBeanProperty1());
    }

    public void testEndpointPropertyBean()
    {
        UMODescriptor d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("appleComponent3");
        Assert.assertNotNull(d);
        Assert.assertNotNull(d.getInboundRouter());
        UMOEndpoint e = (UMOEndpoint)d.getInboundRouter().getEndpoints().get(0);
        Assert.assertNotNull(e);
        Assert.assertEquals("Prop2", e.getProperties().get("testEndpointBeanProperty"));

        d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("orangeComponent");
        Assert.assertNotNull(d);
        UMORouterCatchAllStrategy strategy = d.getInboundRouter().getCatchAllStrategy();
        Assert.assertTrue(strategy instanceof ForwardingCatchAllStrategy);
        UMOConnector conn = strategy.getEndpoint().getConnector();
        Assert.assertTrue(conn instanceof TestConnector);
        Assert.assertEquals("dummyConnector2", conn.getName());
        
    }

    public void testPropertyBeansOnDescriptors()
    {
        UMODescriptor d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("appleComponent3");
        Assert.assertNotNull(d);

        Assert.assertTrue(d.getExceptionListener() instanceof DefaultExceptionStrategy);

        // assertEquals("1.1", d.getVersion());
    }

    public void testPropertyBeansInMaps()
    {
        UMODescriptor d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("appleComponent3");
        Assert.assertNotNull(d);
        Map map = (Map)d.getProperties().get("springMap");
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        List list = (List)d.getProperties().get("springList");
        Assert.assertNotNull(list);
        Assert.assertEquals(2, list.size());
        Set set = (Set)d.getProperties().get("springSet");
        Assert.assertNotNull(set);
        Assert.assertEquals(2, set.size());
        Assert.assertNotNull(d.getProperties().get("springBean"));
    }

    public void testConvertedSpringBeans() throws UMOException
    {
        Assert.assertNotNull(AbstractMuleTestCase.managementContext.getRegistry().lookupModel("main").getComponent("TestComponent"));
    }
}

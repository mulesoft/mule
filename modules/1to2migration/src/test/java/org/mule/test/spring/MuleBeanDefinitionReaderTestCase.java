/*
 * $Id:MuleBeanDefinitionReaderTestCase.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
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
        assertNotNull(c);
        assertEquals("hello",c.getBeanProperty1());
    }

    public void testEndpointPropertyBean()
    {
        UMODescriptor d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("appleComponent3");
        assertNotNull(d);
        assertNotNull(d.getInboundRouter());
        UMOEndpoint e = (UMOEndpoint)d.getInboundRouter().getEndpoints().get(0);
        assertNotNull(e);
        assertEquals("Prop2", e.getProperties().get("testEndpointBeanProperty"));

        d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(d);
        UMORouterCatchAllStrategy strategy = d.getInboundRouter().getCatchAllStrategy();
        assertTrue(strategy instanceof ForwardingCatchAllStrategy);
        UMOConnector conn = strategy.getEndpoint().getConnector();
        assertTrue(conn instanceof TestConnector);
        assertEquals("dummyConnector2", conn.getName());
        
    }

    public void testPropertyBeansOnDescriptors()
    {
        UMODescriptor d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("appleComponent3");
        assertNotNull(d);

        assertTrue(d.getExceptionListener() instanceof DefaultExceptionStrategy);

        // assertEquals("1.1", d.getVersion());
    }

    public void testPropertyBeansInMaps()
    {
        UMODescriptor d = AbstractMuleTestCase.managementContext.getRegistry().lookupService("appleComponent3");
        assertNotNull(d);
        Map map = (Map)d.getProperties().get("springMap");
        assertNotNull(map);
        assertEquals(2, map.size());
        List list = (List)d.getProperties().get("springList");
        assertNotNull(list);
        assertEquals(2, list.size());
        Set set = (Set)d.getProperties().get("springSet");
        assertNotNull(set);
        assertEquals(2, set.size());
        assertNotNull(d.getProperties().get("springBean"));
    }

    public void testConvertedSpringBeans() throws UMOException
    {
        assertNotNull(AbstractMuleTestCase.managementContext.getRegistry().lookupModel("main").getComponent("TestComponent"));
    }
}

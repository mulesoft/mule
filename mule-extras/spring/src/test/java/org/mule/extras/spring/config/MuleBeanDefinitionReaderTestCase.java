/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.providers.vm.VMConnector;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.umo.UMODescriptor;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleBeanDefinitionReaderTestCase extends AbstractConfigBuilderTestCase
{

    public String getConfigResources()
    {
        // A Mule Xml config file and a Spring context file
        return "test-xml-mule-config-split-with-beans.xml,test-xml-mule-config.xml,test-application-context.xml,test-xml-mule-config-split.xml";
    }

    public ConfigurationBuilder getBuilder()
    {
        return new SpringConfigurationBuilder();
    }

    // Test spring bean configs

    public void testConnectorBean()
    {
        VMConnector c = (VMConnector) MuleManager.getInstance().lookupConnector("beanConnector");
        assertNotNull(c);
        assertTrue(c.isQueueEvents());
    }

    public void testEndpointPropertyBean()
    {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("appleComponent3");
        assertNotNull(d);
        assertNotNull(d.getInboundRouter());
        UMOEndpoint e = (UMOEndpoint) d.getInboundRouter().getEndpoints().get(0);
        assertNotNull(e);
        assertEquals("Prop2", e.getProperties().get("testEndpointBeanProperty"));

        d = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
        assertNotNull(d);
//        e = d.getInboundEndpoint();
//        assertNotNull(e);
//        assertEquals(e.getEndpointURI().toString(), MuleManager.getInstance()
//                                                               .getEndpointIdentifiers()
//                                                               .get("Test Queue"));
    }

    public void testPropertyBeansOnDescriptors()
    {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("appleComponent3");
        assertNotNull(d);

        assertTrue(d.getExceptionListener() instanceof DefaultExceptionStrategy);

        // assertEquals("1.1", d.getVersion());
    }

    public void testPropertyBeansInMaps()
    {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("appleComponent3");
        assertNotNull(d);
        Map map = (Map) d.getProperties().get("springMap");
        assertNotNull(map);
        assertEquals(2, map.size());
        List list = (List) d.getProperties().get("springList");
        assertNotNull(list);
        assertEquals(2, list.size());
        Set set = (Set) d.getProperties().get("springSet");
        assertNotNull(set);
        assertEquals(2, set.size());
        assertNotNull(d.getProperties().get("springBean"));
    }
}

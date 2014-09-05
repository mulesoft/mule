/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.mbeans;

import static org.junit.Assert.assertEquals;
import org.mule.api.processor.MessageProcessor;
import org.mule.component.DefaultJavaComponent;
import org.mule.construct.Flow;
import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.module.management.mbean.FlowConstructService;
import org.mule.object.SingletonObjectFactory;

import java.util.ArrayList;
import java.util.Set;

import javax.management.ObjectName;

import org.junit.Test;

public class ServiceServiceTestCase extends AbstractMuleJmxTestCase
{
    @Test
    public void testUndeploy() throws Exception
    {
        final String domainOriginal = "TEST_DOMAIN_1";

        final Flow flow = new Flow("TEST_SERVICE", muleContext);
        SingletonObjectFactory factory = new SingletonObjectFactory(Object.class);
        final DefaultJavaComponent component = new DefaultJavaComponent(factory);
        component.setMuleContext(muleContext);
        flow.setMessageProcessors(new ArrayList<MessageProcessor>());
        flow.getMessageProcessors().add(component);

        muleContext.getRegistry().registerFlowConstruct(flow);
        muleContext.start();

        final FlowConstructService jmxService = new FlowConstructService("TEST_SERVICE", flow.getName(), muleContext,
                                                                         null);
        final ObjectName name = ObjectName.getInstance(domainOriginal + ":type=TEST_SERVICE");
        mBeanServer.registerMBean(jmxService, name);
        Set mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        // Expecting following mbeans to be registered:
        // 1) org.mule.management.mbeans.ServiceService@TEST_DOMAIN_1:type=TEST_SERVICE
        // 2) org.mule.management.mbeans.ServiceStats@TEST_DOMAIN_1:type=org.mule.Statistics,service=TEST_SERVICE
        assertEquals("Unexpected number of components registered in the domain.", 2, mbeans.size());

        mBeanServer.unregisterMBean(name);

        mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}

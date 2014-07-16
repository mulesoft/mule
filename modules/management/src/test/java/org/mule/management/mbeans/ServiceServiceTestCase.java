/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.api.config.ThreadingProfile;
import org.mule.component.DefaultJavaComponent;
import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.module.management.mbean.ServiceService;
import org.mule.object.SingletonObjectFactory;

import java.util.Set;

import javax.management.ObjectName;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServiceServiceTestCase extends AbstractMuleJmxTestCase
{
    @Test
    public void testUndeploy() throws Exception
    {
        final String domainOriginal = "TEST_DOMAIN_1";

        final SedaService service = new SedaService(muleContext);
        service.setName("TEST_SERVICE");
        SingletonObjectFactory factory = new SingletonObjectFactory(Object.class);
        final DefaultJavaComponent component = new DefaultJavaComponent(factory);
        component.setMuleContext(muleContext);
        service.setComponent(component);

        ThreadingProfile defaultThreadingProfile = ThreadingProfile.DEFAULT_THREADING_PROFILE;
        defaultThreadingProfile.setMuleContext(muleContext);
        service.setThreadingProfile(defaultThreadingProfile);
        SedaModel model = new SedaModel();
        model.setMuleContext(muleContext);
        service.setModel(model);
        muleContext.getRegistry().registerModel(model);
        muleContext.getRegistry().registerService(service);
        muleContext.start();

        final ServiceService jmxService = new ServiceService("TEST_SERVICE", muleContext);
        final ObjectName name = ObjectName.getInstance(domainOriginal + ":type=TEST_SERVICE");
        mBeanServer.registerMBean(jmxService, name);
        Set mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        // Expecting following mbeans to be registered:
        // 1) org.mule.management.mbeans.ServiceService@TEST_DOMAIN_1:type=TEST_SERVICE
        // 2) org.mule.management.mbeans.ServiceStats@TEST_DOMAIN_1:type=org.mule.Statistics,service=TEST_SERVICE
        // 3) org.mule.management.mbeans.RouterStats@TEST_DOMAIN_1:type=org.mule.Statistics,service=TEST_SERVICE,router=inbound
        // 4) org.mule.management.mbeans.RouterStats@TEST_DOMAIN_1:type=org.mule.Statistics,service=TEST_SERVICE,router=outbound
        assertEquals("Unexpected number of components registered in the domain.", 4, mbeans.size());

        mBeanServer.unregisterMBean(name);

        mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}

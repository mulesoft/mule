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
package org.mule.management.mbeans;

import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleDescriptor;
import org.mule.tck.AbstractMuleJmxTestCase;

import javax.management.ObjectName;
import java.util.Set;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 *
 * $Id$$
 */
public class ComponentServiceTestCase extends AbstractMuleJmxTestCase
{
    public void testUndeploy() throws Exception
    {
        final String domainOriginal = "TEST_DOMAIN_1";


        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "true");
        MuleManager manager = (MuleManager) getManager();
        final MuleDescriptor descriptor = new MuleDescriptor("TEST_SERVICE");
        descriptor.setImplementation(new Object());
        manager.getModel().registerComponent(descriptor);

        manager.start();
        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "false");

        final ComponentService service = new ComponentService("TEST_SERVICE");
        final ObjectName name = ObjectName.getInstance(domainOriginal + ":type=TEST_SERVICE");
        mBeanServer.registerMBean(service, name);
        Set mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        assertEquals("Unexpected number of components registered in the domain.", 4, mbeans.size());

        mBeanServer.unregisterMBean(name);

        mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}

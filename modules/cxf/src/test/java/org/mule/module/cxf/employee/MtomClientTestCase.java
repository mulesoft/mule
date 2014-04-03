/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.employee;

import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.junit.Rule;
import org.junit.Test;

public class MtomClientTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "mtom-client-conf-flow.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        final EmployeeDirectoryImpl svc = (EmployeeDirectoryImpl) getComponent("employeeDirectoryService");

        muleContext.getClient().dispatch("vm://in", "", null);

        Prober prober = new PollingProber(6000, 500);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return (svc.getInvocationCount() == 1);
            }

            @Override
            public String describeFailure()
            {
                return "Expected invocation count was 1 but actual one was " + svc.getInvocationCount();
            }
        });

        // ensure that an attachment was actually sent.
        assertTrue(AttachmentVerifyInterceptor.HasAttachments);
    }

}


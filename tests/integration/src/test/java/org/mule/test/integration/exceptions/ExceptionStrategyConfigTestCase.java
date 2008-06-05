/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.service.Service;
import org.mule.service.DefaultServiceExceptionStrategy;
import org.mule.tck.FunctionalTestCase;

public class ExceptionStrategyConfigTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-config.xml";
    }

    public void testConfig() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("testService1");
        assertNotNull(service);
        assertNotNull(service.getExceptionListener());
        assertTrue(service.getExceptionListener() instanceof DefaultServiceExceptionStrategy);

        DefaultServiceExceptionStrategy es = (DefaultServiceExceptionStrategy)service.getExceptionListener();
        assertFalse(es.isEnableNotifications());
        assertNotNull(es.getCommitTxFilter());
        assertEquals("java.io.*", es.getCommitTxFilter().getPattern());

        assertNotNull(es.getRollbackTxFilter());
        assertEquals("org.mule.*, javax.*", es.getRollbackTxFilter().getPattern());

    }
}
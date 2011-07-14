/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.api.service.Service;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExceptionStrategyConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/exceptions/exception-strategy-config.xml";
    }

    @Test
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

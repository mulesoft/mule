/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jboss.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class JbossTSNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jbossts-namespacehandler.xml";
    }

    @Test
    public void testNamespaceHandler()
    {
        assertNotNull(muleContext.getTransactionManager());
        assertTrue(muleContext.getTransactionManager().getClass().getName().compareTo("arjuna") > 0);
        // TODO JBossTS now uses different configuration approach, broke props into 3 javabeans, update
        //assertEquals(arjPropertyManager.propertyManager.getProperty("test"),"TEST");
    }
}

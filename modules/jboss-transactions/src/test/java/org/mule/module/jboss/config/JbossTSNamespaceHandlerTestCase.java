/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jboss.config;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JbossTSNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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

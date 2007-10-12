/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.jboss.config;

import org.mule.tck.FunctionalTestCase;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

public class JbossTSNamespaceHandlerTestCase extends FunctionalTestCase
{

    public void testNamespaceHandler()
    {
        assertNotNull(managementContext.getTransactionManager());
        assertTrue(managementContext.getTransactionManager().getClass().getName().compareTo("arjuna") > 0);
        assertEquals(arjPropertyManager.propertyManager.getProperty("test"),"TEST");
    }

    protected String getConfigResources()
    {
        return "jbossts-namespacehandler.xml";
    }
}

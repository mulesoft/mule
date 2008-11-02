/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.module.xml.util.NamespaceManager;
import org.mule.tck.FunctionalTestCase;

public class XmlNamespaceTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "xml-namespace-config.xml";
    }

    public void testGlobalNamespaces() throws Exception
    {
        NamespaceManager manager = (NamespaceManager)muleContext.getRegistry().lookupObject(NamespaceManager.class);
        assertNotNull(manager);
        assertTrue(manager.isIncludeConfigNamespaces());
        assertEquals(4, manager.getNamespaces().size());
    }
}

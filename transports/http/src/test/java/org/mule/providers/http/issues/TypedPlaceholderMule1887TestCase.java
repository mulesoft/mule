/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.issues;

import org.mule.providers.http.AbstractNamespaceHandlerTestCase;
import org.mule.providers.http.HttpConnector;

public class TypedPlaceholderMule1887TestCase extends AbstractNamespaceHandlerTestCase
{

    public TypedPlaceholderMule1887TestCase()
    {
        super("http");
    }

    protected String getConfigResources()
    {
        return "typed-placeholder-mule-1887-test.xml";
    }

    public void testConnectorProperties()
    {
        HttpConnector connector =
                (HttpConnector) managementContext.getRegistry().lookupConnector("httpConnector");
        testBasicProperties(connector);
    }

}
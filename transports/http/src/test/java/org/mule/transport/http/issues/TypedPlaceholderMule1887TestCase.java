/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.issues;

import org.mule.transport.http.AbstractNamespaceHandlerTestCase;
import org.mule.transport.http.HttpConnector;

import org.junit.Test;

public class TypedPlaceholderMule1887TestCase extends AbstractNamespaceHandlerTestCase
{

    public TypedPlaceholderMule1887TestCase()
    {
        super("http");
    }

    @Override
    protected String getConfigResources()
    {
        return "typed-placeholder-mule-1887-test.xml";
    }

    @Test
    public void testConnectorProperties()
    {
        HttpConnector connector =
                (HttpConnector) muleContext.getRegistry().lookupConnector("httpConnector");
        testBasicProperties(connector);
    }

}

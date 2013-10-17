/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.ajax.embedded.AjaxConnector;

import java.net.URL;

import org.junit.Ignore;

public class AjaxEmbeddedConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        AjaxConnector c = new AjaxConnector(muleContext);
        c.setName("test");
        //By default the connector is not started until the servlet container is up.  We start it here because
        //this test looks at the connector lifecycle
        c.setInitialStateStopped(false);
        c.setServerUrl(new URL("http://0.0.0.0:12345"));
        return c;
    }

    @Override
    @Ignore("MULE-7068")
    public void testConnectorLifecycle() throws Exception
    {

    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "{\"value1\" : \"foo\", \"value2\" : \"bar\"}";
    }

    @Override
    public String getTestEndpointURI()
    {
        return "ajax:///request";
    }
}

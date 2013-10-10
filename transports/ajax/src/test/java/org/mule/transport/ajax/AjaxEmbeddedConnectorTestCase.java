/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ajax;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.ajax.embedded.AjaxConnector;

import java.net.URL;

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

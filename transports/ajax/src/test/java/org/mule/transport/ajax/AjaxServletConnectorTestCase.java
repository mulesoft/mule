/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.ajax.container.AjaxServletConnector;

public class AjaxServletConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        AjaxServletConnector c = new AjaxServletConnector(muleContext);
        c.setName("test");
        c.setInitialStateStopped(false);
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
        return "ajax-servlet:///service/request";
    }
}

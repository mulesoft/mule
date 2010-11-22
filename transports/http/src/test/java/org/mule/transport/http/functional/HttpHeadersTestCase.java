/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.http.HttpConstants;

public class HttpHeadersTestCase extends DynamicPortTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "http-headers-config.xml";
    }

    public void testJettyHeaders() throws Exception 
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint", null, null);

        String contentTypeProperty = result.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        assertNotNull(contentTypeProperty); 
        assertEquals("application/x-download", contentTypeProperty);

        String contentDispositionProperty = result.getInboundProperty(HttpConstants.HEADER_CONTENT_DISPOSITION);
        assertNotNull(contentDispositionProperty);
        assertEquals("attachment; filename=foo.zip", contentDispositionProperty);
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }

}

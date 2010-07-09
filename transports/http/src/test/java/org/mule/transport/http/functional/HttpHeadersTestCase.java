/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

public class HttpHeadersTestCase extends FunctionalTestCase
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
        
        String contentTypeProperty = 
            result.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, PropertyScope.OUTBOUND, null);
        assertNotNull(contentTypeProperty); 
        assertEquals("application/x-download", contentTypeProperty); 
        
        String contentDispositionProperty = 
            result.getStringProperty(HttpConstants.HEADER_CONTENT_DISPOSITION, PropertyScope.OUTBOUND, null);
        assertNotNull(contentDispositionProperty);
        assertEquals("attachment; filename=foo.zip", contentDispositionProperty);
    }

}

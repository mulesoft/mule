/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.construct;

import org.mule.api.endpoint.OutboundEndpoint;

import java.net.URI;
import java.util.Collections;

public class UrlWsdlWSProxyTestCase extends AbstractWSProxyTestCase
{
    @SuppressWarnings("unchecked")
    @Override
    protected WSProxy newWSProxy(final OutboundEndpoint testOutboundEndpoint) throws Exception
    {
        return new WSProxy("url-wsdl-ws-proxy", muleContext, directInboundMessageSource,
            testOutboundEndpoint, Collections.EMPTY_LIST, Collections.EMPTY_LIST, new URI("test://test?wsdl"));
    }
}

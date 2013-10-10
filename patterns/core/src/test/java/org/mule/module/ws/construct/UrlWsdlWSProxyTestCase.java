/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ws.construct;

import java.net.URI;
import java.util.Collections;

import org.mule.api.endpoint.OutboundEndpoint;

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

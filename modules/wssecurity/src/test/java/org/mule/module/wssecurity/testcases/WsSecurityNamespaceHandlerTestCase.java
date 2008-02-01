/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.wssecurity.testcases;

import org.mule.api.security.EndpointSecurityFilter;
import org.mule.module.wssecurity.filters.WsSecurityFilter;
import org.mule.tck.FunctionalTestCase;

public class WsSecurityNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "ws-security-namespace-config.xml";
    }

    public void testPropertiesOnFilter() throws Exception
    {
        EndpointSecurityFilter filter = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "endpoint").getSecurityFilter();
        assertTrue(filter instanceof WsSecurityFilter);
        assertNotNull(((WsSecurityFilter)filter).getWsDecryptionFile());
        assertNotNull(((WsSecurityFilter)filter).getWsSignatureFile());
    }
}

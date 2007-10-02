/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.testcases;

import org.mule.extras.wssecurity.filters.WsSecurityFilter;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.security.UMOEndpointSecurityFilter;

public class WsSecurityNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "ws-security-namespace-config.xml";
    }

    public void testPropertiesOnFilter() throws Exception
    {
        UMOEndpointSecurityFilter filter = managementContext.getRegistry().lookupEndpoint(
            "endpoint").getSecurityFilter();
        assertTrue(filter instanceof WsSecurityFilter);
        assertNotNull(((WsSecurityFilter)filter).getWsDecryptionFile());
        assertNotNull(((WsSecurityFilter)filter).getWsSignatureFile());
    }
}

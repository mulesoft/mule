/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.http;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class HttpPollingFunctionalTestCase extends FunctionalTestCase
{

    public void testPollingHttpConnectorSentCredentials() throws Exception
    {    
        MuleClient client = new MuleClient();
        UMOMessage result = client.receive("vm://toclient", 5000);
        assertNotNull(result.getPayload());
    }
    
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/http/mule-http-config.xml,org/mule/test/integration/providers/http/mule-http-polling-config.xml";
    }
}
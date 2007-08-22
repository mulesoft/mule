/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class HttpPollingFunctionalTestCase extends FunctionalTestCase
{

    public void testPollingHttpConnector() throws Exception
    {
        // this is broken because of MULE-1770
        MuleClient client = new MuleClient();
        UMOMessage result = client.receive("vm://toclient", 5000);
        assertNotNull(result.getPayload());
    }
    
    protected String getConfigResources()
    {
        return "mule-http-polling-config.xml";
    }

}
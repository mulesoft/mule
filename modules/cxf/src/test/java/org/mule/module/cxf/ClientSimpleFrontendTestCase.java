/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ClientSimpleFrontendTestCase extends FunctionalTestCase
{

    public void testEchoWsdl() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("vm://test", "some payload", null);

        assertNotNull(result.getPayload());
        assertEquals("Hello some payload", result.getPayload());
    }

    @Override
    protected String getConfigResources()
    {
        return "aegis-conf.xml";
    }

}



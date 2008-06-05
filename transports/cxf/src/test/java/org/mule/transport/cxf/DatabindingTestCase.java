/*
 * $Id: CxfBasicTestCase.java 11405 2008-03-18 00:13:00Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class DatabindingTestCase extends FunctionalTestCase
{

    public void testEchoWsdl() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.request("http://localhost:63081/services/Echo?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    protected String getConfigResources()
    {
        return "databinding-conf.xml";
    }

}

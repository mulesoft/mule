/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.soap.axis;

import org.apache.axis.AxisProperties;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SoapDocLitFunctionalTestCase extends FunctionalTestCase {

    protected String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis/axis-doc-lit-mule-config.xml";
    }

    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient();
         AxisProperties.setProperty("axis.doAutoTypes", "true");
        UMOMessage m = client.send("axis:http://localhost:38008/mule/mycomponent?method=getPerson", "Fred", null);
        assertNotNull(m);
    }
}

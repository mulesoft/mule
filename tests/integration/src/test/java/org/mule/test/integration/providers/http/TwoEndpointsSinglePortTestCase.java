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
*
*/
package org.mule.test.integration.providers.http;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TwoEndpointsSinglePortTestCase extends FunctionalTestCase
 {
    public TwoEndpointsSinglePortTestCase() {
        setDisposeManagerPerSuite(true);
    }

     protected String getConfigResources() {
         return "org/mule/test/integration/providers/http/two-endpoints-single-port.xml";
     }

     public void testSendToEach() throws Exception {

        sendWithResponse("http://localhost:8081/mycomponent1", "test", "mycomponent1", 10);
        sendWithResponse("http://localhost:8081/mycomponent2", "test", "mycomponent2", 10);
    }

    public void testSendToEachWithBadEndpoint() throws Exception {

        MuleClient client = new MuleClient();

        sendWithResponse("http://localhost:8081/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:8081/mycomponent2", "test", "mycomponent2", 5);

        UMOMessage result = client.send("http://localhost:8081/mycomponent-notfound", "test", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(404, result.getIntProperty("http.status", 0));

        //Test that after the exception the endpoints still receive events
        sendWithResponse("http://localhost:8081/mycomponent1", "test", "mycomponent1", 5);
        sendWithResponse("http://localhost:8081/mycomponent2", "test", "mycomponent2", 5);
    }

    protected void sendWithResponse(String endpoint, String message, String response, int noOfMessages) throws UMOException {
        MuleClient client = new MuleClient();

        List results = new ArrayList();
        for (int i = 0; i < noOfMessages; i++) {
            results.add(client.send(endpoint, message, null).getPayload());
        }

        assertEquals(noOfMessages, results.size());
        for (int i = 0; i < noOfMessages; i++) {
            assertEquals(response, results.get(i).toString());
        }
    }
}

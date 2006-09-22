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

import org.mule.providers.http.HttpConnector;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:jesper@selskabet.org">Jesper Steen Møller</a>
 * @version $Revision$
 */
public class HttpBadEncodingFunctionalTestCase extends HttpEncodingFunctionalTestCase
{
    UMOMessage reply;

    protected void sendTestData(int iterations) throws Exception {
        reply = send(getInDest().getAddress(), TEST_MESSAGE, "text/plain;charset=UTFF-912");
    }
    protected void receiveAndTestResults() throws Exception
    {
        assertNotNull(reply);
        assertEquals("500", reply.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertNotNull(reply.getExceptionPayload());
        
        callbackCalled=true;
    }

    
}

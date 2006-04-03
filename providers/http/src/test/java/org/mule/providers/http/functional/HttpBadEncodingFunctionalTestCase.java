/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers.http.functional;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractConnector;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;

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

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.NullPayload;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

import java.io.FileOutputStream;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FtpConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{

    protected static final String USERNAME = "mule";
    protected static final String PASSWORD = "mulepwd";
    protected static final String HOSTNAME = "localhost";
    protected static final String ADDRESS = "test";

    protected static transient Log logger = LogFactory.getLog(FtpConnectorFunctionalTestCase.class);

    public void testClient() throws Exception
    {
        UMOImmutableEndpoint endpoint = new ImmutableMuleEndpoint(getOutDest().toString(), false);
        FtpConnector c = (FtpConnector) createConnector();
        c.initialise();
        UMOMessageDispatcher d = c.getDispatcher(endpoint);
        UMOEvent e = getTestEvent(new NullPayload(), endpoint);
        d.dispatch(e);
    }

    protected void sendTestData(int iterations) throws Exception
    {
        for (int i = 0; i < 100; i++) {
            FileOutputStream fos = new FileOutputStream("c:\\temp\\ftp\\test\\test" + i + ".txt");
            fos.write(("Hello" + i).getBytes());
            fos.close();
        }
    }

    protected void receiveAndTestResults() throws Exception
    {
        Thread.sleep(5000);
    }

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("ftp://" + USERNAME + ":" + PASSWORD + "@" + HOSTNAME + "/" + ADDRESS);
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return getInDest();
    }

    protected UMOConnector createConnector() throws Exception
    {
        FtpConnector connector = new FtpConnector();
        connector.setName("testFtp");
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        return connector;
    }

}

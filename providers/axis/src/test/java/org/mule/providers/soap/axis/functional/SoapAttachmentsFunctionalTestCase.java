/*
 * $Id$ 
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers.soap.axis.functional;

import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.model.seda.SedaModel;
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.providers.soap.axis.AxisMessageDispatcher;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import java.io.File;

/**
 * @author <a href="mailto:risears@gmail.com">Rick Sears</a>
 * @version $Revision$
 */
public class SoapAttachmentsFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    protected void doSetUp() throws Exception
    {
        manager = MuleManager.getInstance();
        // Make sure we are running synchronously
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration()
                   .getPoolingProfile()
                   .setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);

        manager.setModel(new SedaModel());
        callbackCalled = false;
        callbackCount = 0;
        connector = createConnector();
    }

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("axis:http://localhost:60198/mule/services");
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return null;
    }

    protected UMOConnector createConnector() throws Exception
    {
        AxisConnector connector = new AxisConnector();
        connector.setName("testAxis");
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        return connector;
    }

    public void testSend() throws Exception
    {
        descriptor = getTestDescriptor("testComponent", SoapAttachmentsFunctionalTestComponent.class.getName());

        initialiseComponent(descriptor, null);
        // Start the server
        MuleManager.getInstance().start();

        sendTestData(5);

        afterInitialise();

        receiveAndTestResults();
    }

    protected void sendTestData(int iterations) throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint("axis:http://localhost:60198/mule/services/testComponent?method=receiveMessageWithAttachments", false);

        AxisMessageDispatcher client = new AxisMessageDispatcher(ep);
        for(int i = 0; i < iterations; i++) {
            UMOMessage msg = new MuleMessage("testPayload");
            File tempFile = File.createTempFile("test", ".att");
            tempFile.deleteOnExit();
            msg.addAttachment("testAttachment", new DataHandler(new FileDataSource(tempFile)));
            MuleEvent event = new MuleEvent(msg, ep, null, true);
            UMOMessage result = client.send(event);
            assertNotNull(result);
            assertNotNull(result.getPayload());
            assertEquals(result.getPayloadAsString(), "Done");
            callbackCount++;
        }
    }

    protected void receiveAndTestResults() throws Exception
    {
        assertEquals(5, callbackCount);
    }
}
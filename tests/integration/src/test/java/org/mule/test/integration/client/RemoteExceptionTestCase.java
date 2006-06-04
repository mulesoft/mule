/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.MuleException;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transformers.simple.ByteArrayToString;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.transformer.TransformerException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RemoteExceptionTestCase extends FunctionalTestCase {

    protected String getConfigResources() {
        return "";
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        //Test component 1. Will be used to create a transformer exceotion
        MuleEndpoint ep = new MuleEndpoint("vm://test.queue.1", true);
        ep.setTransformer(new ByteArrayToString());
        builder.registerComponent(FunctionalTestComponent.class.getName(), "testComponent1", ep, null, null);

        //Test component 2. Will be used to create an exception thrown from within the component
        MuleEndpoint ep2 = new MuleEndpoint("vm://test.queue.2", true);
        Map props = new HashMap();
        props.put("throwException", "true");
        builder.registerComponent(FunctionalTestComponent.class.getName(), "testComponent2", ep2, null, props);

        builder.createStartedManager(true, "http://localhost:5555");
        return builder;
    }

    public void testClientTransformerException() throws Exception {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("http://localhost:5555");
        UMOMessage result = dispatcher.sendRemote("vm://test.queue.1", new Date(), null);
        assertNotNull(result);
        UMOExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof TransformerException);
    }

    public void testClientMalformedEndpointException() throws Exception {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("http://localhost:5555");
        UMOMessage result = dispatcher.sendRemote("test.queue.2", new Date(), null);
        assertNotNull(result);
        UMOExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof MalformedEndpointException);
    }

    public void testClientComponentException() throws Exception {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("http://localhost:5555");
        UMOMessage result = dispatcher.sendRemote("vm://test.queue.2", new Date(), null);
        assertNotNull(result);
        UMOExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof MuleException);
        assertEquals("Functional Test Component Exception", exceptionPayload.getRootException().getMessage());
    }

}

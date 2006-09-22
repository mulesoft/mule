/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.providers.email.transformers.PlainTextDataSource;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

import javax.activation.DataHandler;

import junit.framework.TestCase;

/**
 * @author jbraam
 */
public class AttachmentsPropagationTestCase extends TestCase implements EventCallback {

    QuickConfigurationBuilder builder;

    protected void setUp() throws Exception {
        builder = new QuickConfigurationBuilder();
        builder.createStartedManager(false, null);

        UMOEndpoint vmSingle = builder.createEndpoint("vm://Single", "SingleEndpoint", true);
        UMOEndpoint vmChained = builder.createEndpoint("vm://Chained", "ChainedEndpoint", true);

        FunctionalTestComponent single = new FunctionalTestComponent();
        single.setEventCallback(this);
        FunctionalTestComponent chained = new FunctionalTestComponent();
        chained.setEventCallback(this);
        builder.registerComponentInstance(single, "SINGLE", vmSingle.getEndpointURI());
        builder.registerComponentInstance(chained, "CHAINED", vmChained.getEndpointURI(), vmSingle.getEndpointURI());

    }

    protected void tearDown() throws Exception {
        builder.disposeCurrent();
    }

    public void eventReceived(UMOEventContext context, Object Component)
            throws Exception {
        UMOMessage message = context.getMessage();
        // add an attachment, named after the componentname...
        message.addAttachment(context.getComponentDescriptor().getName(),
                new DataHandler(new PlainTextDataSource("text/plain", "<content>")));

        // return the list of attachment names
        FunctionalTestComponent fc = (FunctionalTestComponent) Component;
        fc.setReturnMessage(message.getAttachmentNames().toString());
    }

    public void testSingleComponentKnowsAttachments() throws Exception {

        MuleClient client = new MuleClient();
        UMOMessage result = client.send("vm://Single", "", null);
        assertNotNull(result);

        // expect SINGLE attachment from SINGLE component
        assertEquals("[SINGLE]", result.getPayloadAsString());
    }

    public void testChainedComponentKnowsAttachments() throws Exception {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("vm://Chained", "", null);
        assertNotNull(result);

         // expect CHAINED attachment from CHAINED component
         // and SINGLE attachment from SINGLE component
        assertEquals("[SINGLE, CHAINED]", result.getPayloadAsString());
    }

    public void testClientReceivesAttachments() throws Exception {
        // a MuleClient should be able to receive attachments
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("vm://Single", "", null);
        assertNotNull(result);

        // expect SINGLE attachment from SINGLE component
        assertEquals("[SINGLE]", result.getPayloadAsString());
        assertNotNull(result.getAttachment("SINGLE"));
        assertEquals("<content>", result.getAttachment("SINGLE").getContent().toString());

        result = client.send("vm://Chained", "", null);
        assertNotNull(result);

         // expect SINGLE and CHAINED attachments
        assertEquals("[SINGLE, CHAINED]", result.getPayloadAsString());
        assertNotNull(result.getAttachment("SINGLE"));
        assertEquals("<content>", result.getAttachment("SINGLE").getContent().toString());
        assertNotNull(result.getAttachment("CHAINED"));
        assertEquals("<content>", result.getAttachment("CHAINED").getContent().toString());
    }
}
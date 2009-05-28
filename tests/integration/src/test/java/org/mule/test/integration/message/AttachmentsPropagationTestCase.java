/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.message;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.email.transformers.PlainTextDataSource;

import javax.activation.DataHandler;

public class AttachmentsPropagationTestCase extends AbstractMuleTestCase implements EventCallback
{
    public void testSanity()
    {
        fail("Convert this test to an XML-based configuration");
    }    

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // TODO Convert this to an XML config
//        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("vm://Single", muleContext);
//        endpointBuilder.setName("SingleEndpoint");
//        ImmutableEndpoint vmSingle = muleContext.getRegistry()
//            .lookupEndpointFactory()
//            .getOutboundEndpoint(endpointBuilder, muleContext);
//        
//        EndpointBuilder endpointBuilder2 = new EndpointURIEndpointBuilder("vm://Single", muleContext);
//        endpointBuilder2.setName("ChainedEndpoint");
//        ImmutableEndpoint vmChained = muleContext.getRegistry()
//            .lookupEndpointFactory()
//            .getOutboundEndpoint(endpointBuilder2, muleContext);
//        
//        FunctionalTestComponent single = new FunctionalTestComponent();
//        single.setEventCallback(this);
//        FunctionalTestComponent chained = new FunctionalTestComponent();
//        chained.setEventCallback(this);
//        builder.registerComponentInstance(single, "SINGLE", vmSingle.getEndpointURI());
//        builder.registerComponentInstance(chained, "CHAINED", vmChained.getEndpointURI(), vmSingle
//            .getEndpointURI());
    }

    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        MuleMessage message = context.getMessage();
        // add an attachment, named after the componentname...
        message.addAttachment(context.getService().getName(), new DataHandler(
            new PlainTextDataSource("text/plain", "<content>")));

        // return the list of attachment names
        FunctionalTestComponent fc = (FunctionalTestComponent) component;
        fc.setReturnData(message.getAttachmentNames().toString());
    }

    public void testSingleComponentKnowsAttachments() throws Exception
    {

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://Single", "", null);
        assertNotNull(result);

        // expect SINGLE attachment from SINGLE service
        assertEquals("[SINGLE]", result.getPayloadAsString());
    }

    public void testChainedComponentKnowsAttachments() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://Chained", "", null);
        assertNotNull(result);

        // expect CHAINED attachment from CHAINED service
        // and SINGLE attachment from SINGLE service
        assertEquals("[SINGLE, CHAINED]", result.getPayloadAsString());
    }

    public void testClientReceivesAttachments() throws Exception
    {
        // a MuleClient should be able to receive attachments
        MuleClient client = new MuleClient();

        MuleMessage result = client.send("vm://Single", "", null);
        assertNotNull(result);

        // expect SINGLE attachment from SINGLE service
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

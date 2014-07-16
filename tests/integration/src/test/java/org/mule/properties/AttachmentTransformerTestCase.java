/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;

import javax.activation.DataHandler;

import org.junit.Test;

public class AttachmentTransformerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/attachment-transformer-test-case.xml";
    }

    @Test
    public void testAddAttachment() throws Exception
    {
        runScenario("addAttachment");
    }

    @Test
    public void testAddAttachmentUsingExpressionKey() throws Exception
    {
        runScenario("addAttachmentUsingExpressionKey");
    }

    @Test
    public void testAddAttachmentUsingExpressionContentType() throws Exception
    {
        runScenario("addAttachmentUsingExpressionContentType");
    }

    @Test
    public void testRemoveAttachment() throws Exception
    {
        runScenario("removeAttachment");
    }

    @Test
    public void testRemoveAttachmentsUsingExpression() throws Exception
    {
        runScenario("removeAttachmentUsingExpression");
    }

    @Test
    public void testRemoveAttachmentsUsingRegex() throws Exception
    {
        runScenario("removeAttachmentUsingRegex");
    }

    @Test
    public void testRemoveAllAttachments() throws Exception
    {
        runScenario("removeAllAttachments");
    }

    @Test
    public void testEnrichAttachment() throws Exception
    {
        runScenario("enrichAttachment");
    }

    @Test
    public void testEnrichAttachmentUsingDataHandler() throws Exception
    {
        runScenario("enrichAttachmentUsingDataHandler");
    }

    @Test
    public void testEnrichAttachmentWithoutContentType() throws Exception
    {
        runScenario("enrichAttachmentWithoutContentType");
    }
    
    @Test
    public void testCopyAttachments() throws Exception
    {
        runScenario("copyAttachments");
    }

    @Test
    public void testCopyAttachmentsWithWildcard() throws Exception
    {
        runScenario("copyAttachmentsWithWildcard");
    }


    public void runScenario(String flowName) throws Exception
    {
        DataHandler attach = new DataHandler("attachContent","plain/text");
        DataHandler attach2 = new DataHandler("attachContent","plain/text");
        DataHandler attach22 = new DataHandler("attachContent","plain/text");
        HashMap<String, DataHandler> attachments = new HashMap<String, DataHandler>();
        attachments.put("attach",attach);
        attachments.put("attach2",attach2);
        attachments.put("attach22",attach22);
        MuleMessage message = new DefaultMuleMessage("data", null, attachments,muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());
        Flow flow = (Flow) getFlowConstruct(flowName);
        flow.process(event);
        FlowAssert.verify(flowName);
    }

}

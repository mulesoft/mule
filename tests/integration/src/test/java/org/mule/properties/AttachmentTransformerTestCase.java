/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

public class AttachmentTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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

    /*@Test
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
    }*/


    public void runScenario(String flowName) throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());
        Flow flow = (Flow) getFlowConstruct(flowName);
        flow.process(event);
        FlowAssert.verify(flowName);
    }

}

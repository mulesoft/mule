/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.xml.routing.XmlMessageSplitter;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.Mock;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RoundRobinXmlSplitterTestCase extends AbstractMuleContextTestCase
{
    private OutboundEndpoint endpoint1;
    private OutboundEndpoint endpoint2;
    private OutboundEndpoint endpoint3;
    private OutboundEndpoint endpoint4;
    private OutboundEndpoint endpoint5;
    private OutboundEndpoint endpoint6;
    private Mock mockendpoint1;
    private Mock mockendpoint2;
    private Mock mockendpoint3;
    private Mock mockendpoint4;
    private Mock mockendpoint5;
    private Mock mockendpoint6;
    private XmlMessageSplitter asyncXmlSplitter;
    private XmlMessageSplitter syncXmlSplitter;

    @Override
    protected void doSetUp() throws Exception
    {
        // setup async targets
        endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);

        // setup sync targets
        endpoint4 = getTestOutboundEndpoint("Test4Endpoint", "test://endpointUri.4?exchangePattern=request-response");
        endpoint5 = getTestOutboundEndpoint("Test5Endpoint", "test://endpointUri.5?exchangePattern=request-response");
        endpoint6 = getTestOutboundEndpoint("Test6Endpoint", "test://endpointUri.6?exchangePattern=request-response");
        mockendpoint4 = RouterTestUtils.getMockEndpoint(endpoint4);
        mockendpoint5 = RouterTestUtils.getMockEndpoint(endpoint5);
        mockendpoint6 = RouterTestUtils.getMockEndpoint(endpoint6);

        // setup async splitter
        asyncXmlSplitter = new XmlMessageSplitter();
        asyncXmlSplitter.setValidateSchema(true);
        asyncXmlSplitter.setExternalSchemaLocation("purchase-order.xsd");
        asyncXmlSplitter.setMuleContext(muleContext);

        // The xml document declares a default namespace, thus
        // we need to workaround it by specifying it both in
        // the namespaces and in the splitExpression
        Map namespaces = new HashMap();
        namespaces.put("e", "http://www.example.com");
        asyncXmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        asyncXmlSplitter.setNamespaces(namespaces);
        asyncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint1.proxy());
        asyncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint2.proxy());
        asyncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint3.proxy());

        // setup sync splitter
        syncXmlSplitter = new XmlMessageSplitter();
        syncXmlSplitter.setMuleContext(muleContext);
        syncXmlSplitter.setValidateSchema(true);
        syncXmlSplitter.setExternalSchemaLocation("purchase-order.xsd");
        syncXmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");

        syncXmlSplitter.setNamespaces(namespaces);
        syncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint4.proxy());
        syncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint5.proxy());
        syncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint6.proxy());
    }

    @Test
    public void testStringPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order2.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    @Test
    public void testDom4JDocumentPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order2.xml", getClass());
        Document doc = DocumentHelper.parseText(payload);
        internalTestSuccessfulXmlSplitter(doc);
    }

    @Test
    public void testByteArrayPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order2.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload.getBytes());
    }

    private void internalTestSuccessfulXmlSplitter(Object payload) throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        session.matchAndReturn("setFlowConstruct", RouterTestUtils.getArgListCheckerFlowConstruct(), null);
        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        assertTrue(asyncXmlSplitter.isMatch(message));
        final RoundRobinXmlSplitterTestCase.ItemNodeConstraint itemNodeConstraint = new RoundRobinXmlSplitterTestCase.ItemNodeConstraint();
        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint3.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        asyncXmlSplitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
        mockendpoint1.verify();
        mockendpoint2.verify();
        mockendpoint3.verify();

        message = new DefaultMuleMessage(payload, muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);
        assertTrue(syncXmlSplitter.isMatch(message));
        mockendpoint4.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint5.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint6.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint4.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleEvent result = syncXmlSplitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertNotNull(result);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(4, ((MuleMessageCollection) resultMessage).size());
        mockendpoint4.verify();
        mockendpoint5.verify();
        mockendpoint6.verify();
    }

    @Test
    public void testXsdNotFoundThrowsException() throws Exception
    {
        final String invalidSchemaLocation = "non-existent.xsd";
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());

        XmlMessageSplitter splitter = new XmlMessageSplitter();
        splitter.setValidateSchema(true);
        splitter.setExternalSchemaLocation(invalidSchemaLocation);
        splitter.setMuleContext(muleContext);
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());

        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        assertTrue(splitter.isMatch(message));
        try
        {
            splitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
            fail("Should have thrown an exception, because XSD is not found.");
        }
        catch (IllegalArgumentException iaex)
        {
            assertTrue("Wrong exception?", iaex.getMessage().indexOf(
                    "Couldn't find schema at " + invalidSchemaLocation) != -1);
        }
        session.verify();
    }


    @Test
    public void testInvalidXmlPayloadThrowsException() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());

        XmlMessageSplitter splitter = new XmlMessageSplitter();
        splitter.setMuleContext(muleContext);      
        MuleMessage message = new DefaultMuleMessage("This is not XML.", muleContext);

        try
        {
            splitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
            fail("No exception thrown.");
        }
        catch (IllegalArgumentException iaex)
        {
            assertTrue("Wrong exception message.", iaex.getMessage().startsWith(
                    "Failed to initialise the payload: "));
        }

    }

    private class ItemNodeConstraint implements Constraint
    {
        public boolean eval(Object o)
        {
            final MuleMessage message = (MuleMessage) o;
            final Object payload = message.getPayload();
            assertTrue("Wrong class type for node.", payload instanceof Document);

            Document node = (Document) payload;

            final String partNumber = node.getRootElement().attributeValue("partNum");

            return "872-AA".equals(partNumber) || "926-AA".equals(partNumber) || "126-AA".equals(partNumber)
                    || "226-AA".equals(partNumber);
        }
    }
}

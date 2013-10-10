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
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.xml.routing.XmlMessageSplitter;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;
import org.mule.routing.CorrelationMode;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.Mock;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XmlMessageSplitterTestCase extends AbstractMuleContextTestCase
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
        // setup sync targets
        endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);

        // setup async targets
        endpoint4 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1?exchangePattern=request-response");
        endpoint5 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2?exchangePattern=request-response");
        endpoint6 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3?exchangePattern=request-response");
        mockendpoint4 = RouterTestUtils.getMockEndpoint(endpoint4);
        mockendpoint5 = RouterTestUtils.getMockEndpoint(endpoint5);
        mockendpoint6 = RouterTestUtils.getMockEndpoint(endpoint6);

        // setup sync splitter
        syncXmlSplitter = new XmlMessageSplitter();
        syncXmlSplitter.setValidateSchema(true);
        syncXmlSplitter.setExternalSchemaLocation("purchase-order.xsd");

        // The xml document declares a default namespace, thus
        // we need to workaround it by specifying it both in
        // the namespaces and in the splitExpression
        Map namespaces = new HashMap();
        namespaces.put("e", "http://www.example.com");
        syncXmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        syncXmlSplitter.setNamespaces(namespaces);
        syncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint4.proxy());
        syncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint5.proxy());
        syncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint6.proxy());

        // setup async splitter
        asyncXmlSplitter = new XmlMessageSplitter();
        asyncXmlSplitter.setValidateSchema(true);
        asyncXmlSplitter.setExternalSchemaLocation("purchase-order.xsd");

        asyncXmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        asyncXmlSplitter.setNamespaces(namespaces);
        asyncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint1.proxy());
        asyncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint2.proxy());
        asyncXmlSplitter.addRoute((OutboundEndpoint) mockendpoint3.proxy());

        syncXmlSplitter.setMuleContext(muleContext);
        asyncXmlSplitter.setMuleContext(muleContext);
        syncXmlSplitter.initialise();
        asyncXmlSplitter.initialise();
    }

    @Test
    public void testStringPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    @Test
    public void testStringPayloadXmlMessageSplitterWithoutXsd() throws Exception
    {
        syncXmlSplitter.setExternalSchemaLocation(null);
        syncXmlSplitter.setValidateSchema(false);
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    @Test
    public void testDom4JDocumentPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        Document doc = DocumentHelper.parseText(payload);
        internalTestSuccessfulXmlSplitter(doc);
    }

    @Test
    public void testW3CDocumentPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        Source sourceDoc = XMLUtils.toXmlSource( XMLInputFactory.newInstance(), true, payload);
        DOMResult result = new DOMResult();
        Transformer transformer = XMLUtils.getTransformer();
        transformer.transform(sourceDoc, result);
        internalTestSuccessfulXmlSplitter(result.getNode());
    }

    @Test
    public void testByteArrayPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload.getBytes());
    }

    @Test
    public void testByteArrayPayloadCorrelateNever() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        syncXmlSplitter.setEnableCorrelation(CorrelationMode.NEVER);
        asyncXmlSplitter.setEnableCorrelation(CorrelationMode.NEVER);
        internalTestSuccessfulXmlSplitter(payload.getBytes());
    }

    private void internalTestSuccessfulXmlSplitter(Object payload) throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        session.matchAndReturn("setFlowConstruct", RouterTestUtils.getArgListCheckerFlowConstruct(), null);
        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        assertTrue(asyncXmlSplitter.isMatch(message));
        final ItemNodeConstraint itemNodeConstraint = new ItemNodeConstraint();
        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        asyncXmlSplitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy()));
        mockendpoint1.verify();
        mockendpoint2.verify();

        message = new DefaultMuleMessage(payload, muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null);
        mockendpoint4.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint5.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleEvent result = syncXmlSplitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy()));
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertNotNull(result);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(2, ((MuleMessageCollection) resultMessage).size());
        mockendpoint4.verify();
        mockendpoint5.verify();
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

        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());

        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        assertTrue(splitter.isMatch(message));
        try
        {
            splitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy()));
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
    public void testInvalidPayloadTypeThrowsException() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());

        XmlMessageSplitter splitter = new XmlMessageSplitter();

        MuleMessage message = new DefaultMuleMessage(new Exception(), muleContext);

        try
        {
            splitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy()));
            fail("No exception thrown.");
        }
        catch (IllegalArgumentException iaex)
        {
            assertTrue("Wrong exception message.", iaex.getMessage().startsWith(
                    "Failed to initialise the payload: "));
        }
    }

    @Test
    public void testInvalidXmlPayloadThrowsException() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());

        XmlMessageSplitter splitter = new XmlMessageSplitter();

        MuleMessage message = new DefaultMuleMessage("This is not XML.", muleContext);

        try
        {
            splitter.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy()));
            fail("No exception thrown.");
        }
        catch (IllegalArgumentException iaex)
        {
            assertTrue("Wrong exception message.", iaex.getMessage().startsWith(
                    "Failed to initialise the payload: "));
        }
    }

    @Test
    public void testGlobalNamespaceManagerLookup() throws Exception
    {
        // clear any configured namespaces
        syncXmlSplitter.setNamespaces(null);
        asyncXmlSplitter.setNamespaces(null);

        // configure a global namespace manager
        NamespaceManager namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
        if (namespaceManager == null)
        {
            namespaceManager = new NamespaceManager();
            muleContext.getRegistry().registerObject(MuleProperties.OBJECT_MULE_NAMESPACE_MANAGER, namespaceManager);
        }
        Map namespaces = new HashMap();
        namespaces.put("e", "http://www.example.com");
        syncXmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        asyncXmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        namespaceManager.setNamespaces(namespaces);

        // re-init splitters
        syncXmlSplitter.initialise();
        asyncXmlSplitter.initialise();

        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    private class ItemNodeConstraint implements Constraint
    {
        public boolean eval(Object o)
        {
            final MuleMessage message = (MuleMessage) o;
            final Object payload = message.getPayload();
            assertTrue("Wrong class type for node.", payload instanceof Document);

            // MULE-2963
            if (syncXmlSplitter.enableCorrelation == CorrelationMode.NEVER
                || asyncXmlSplitter.enableCorrelation == CorrelationMode.NEVER)
            {
                assertEquals(-1, message.getCorrelationGroupSize());
            }
            else
            {
                // the purchase order document contains two parts
                assertEquals(2, message.getCorrelationGroupSize());
            }

            Document node = (Document) payload;
            final String partNumber = node.getRootElement().attributeValue("partNum");
            return "872-AA".equals(partNumber) || "926-AA".equals(partNumber);
        }
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.xml.routing.XmlMessageSplitter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.util.IOUtils;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class XmlMessageSplitterTestCase extends AbstractMuleTestCase
{
    private OutboundEndpoint endpoint1;
    private OutboundEndpoint endpoint2;
    private OutboundEndpoint endpoint3;
    private OutboundEndpoint endpoint4;
    private OutboundEndpoint endpoint5;
    private OutboundEndpoint endpoint6;
    private XmlMessageSplitter asyncXmlSplitter;
    private XmlMessageSplitter syncXmlSplitter;

    // @Override
    protected void doSetUp() throws Exception
    {
        // setup sync endpoints
        endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");

        // setup async endpoints
        endpoint4 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1?synchronous=true");
        endpoint5 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2?synchronous=true");
        endpoint6 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3?synchronous=true");

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
        syncXmlSplitter.addEndpoint(endpoint4);
        syncXmlSplitter.addEndpoint(endpoint5);
        syncXmlSplitter.addEndpoint(endpoint6);

        // setup async splitter
        asyncXmlSplitter = new XmlMessageSplitter();
        asyncXmlSplitter.setValidateSchema(true);
        asyncXmlSplitter.setExternalSchemaLocation("purchase-order.xsd");

        asyncXmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        asyncXmlSplitter.setNamespaces(namespaces);
        asyncXmlSplitter.addEndpoint(endpoint1);
        asyncXmlSplitter.addEndpoint(endpoint2);
        asyncXmlSplitter.addEndpoint(endpoint3);
    }

    public void testStringPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    public void testStringPayloadXmlMessageSplitterWithoutXsd() throws Exception
    {
        syncXmlSplitter.setExternalSchemaLocation(null);
        syncXmlSplitter.setValidateSchema(false);
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    public void testDom4JDocumentPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        Document doc = DocumentHelper.parseText(payload);
        internalTestSuccessfulXmlSplitter(doc);
    }

    public void testByteArrayPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload.getBytes());
    }

    public void testByteArrayPayloadCorrelateNever() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        syncXmlSplitter.setEnableCorrelation(AbstractOutboundRouter.ENABLE_CORRELATION_NEVER);
        asyncXmlSplitter.setEnableCorrelation(AbstractOutboundRouter.ENABLE_CORRELATION_NEVER);
        internalTestSuccessfulXmlSplitter(payload.getBytes());
    }

    private void internalTestSuccessfulXmlSplitter(Object payload) throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        MuleMessage message = new DefaultMuleMessage(payload);

        assertTrue(asyncXmlSplitter.isMatch(message));
        final ItemNodeConstraint itemNodeConstraint = new ItemNodeConstraint();
        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint2)));
        asyncXmlSplitter.route(message, (MuleSession) session.proxy(), false);
        session.verify();

        message = new DefaultMuleMessage(payload);

        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());

        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint4)), message);
        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint5)), message);
        //Note as I have disabled 'honourSynchronicity' (MULE-3299) so setting the sync flag to true or false
        //makes no difference.  See issue for more info.
        MuleMessage result = syncXmlSplitter.route(message, (MuleSession) session.proxy(), false);
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        assertEquals(2, ((MuleMessageCollection) result).size());
        session.verify();
    }

    public void testXsdNotFoundThrowsException() throws Exception
    {
        final String invalidSchemaLocation = "non-existent.xsd";
        Mock session = MuleTestUtils.getMockSession();

        XmlMessageSplitter splitter = new XmlMessageSplitter();
        splitter.setValidateSchema(true);
        splitter.setExternalSchemaLocation(invalidSchemaLocation);

        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());

        MuleMessage message = new DefaultMuleMessage(payload);

        assertTrue(splitter.isMatch(message));
        try
        {
            splitter.route(message, (MuleSession) session.proxy(), false);
            fail("Should have thrown an exception, because XSD is not found.");
        }
        catch (IllegalArgumentException iaex)
        {
            assertTrue("Wrong exception?", iaex.getMessage().indexOf(
                    "Couldn't find schema at " + invalidSchemaLocation) != -1);
        }
        session.verify();
    }


    public void testInvalidPayloadTypeThrowsException() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        XmlMessageSplitter splitter = new XmlMessageSplitter();

        MuleMessage message = new DefaultMuleMessage(new Exception());

        try
        {
            splitter.route(message, (MuleSession) session.proxy(), false);
            fail("No exception thrown.");
        }
        catch (IllegalArgumentException iaex)
        {
            assertTrue("Wrong exception message.", iaex.getMessage().startsWith(
                    "Failed to initialise the payload: "));
        }

    }

    public void testInvalidXmlPayloadThrowsException() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        XmlMessageSplitter splitter = new XmlMessageSplitter();

        MuleMessage message = new DefaultMuleMessage("This is not XML.");

        try
        {
            splitter.route(message, (MuleSession) session.proxy(), false);
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

            // MULE-2963
            if (syncXmlSplitter.enableCorrelation == AbstractOutboundRouter.ENABLE_CORRELATION_NEVER ||
                    asyncXmlSplitter.enableCorrelation == AbstractOutboundRouter.ENABLE_CORRELATION_NEVER)
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

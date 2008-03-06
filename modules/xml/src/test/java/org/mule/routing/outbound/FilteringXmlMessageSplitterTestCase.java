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
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.xml.routing.FilteringXmlMessageSplitter;
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

public class FilteringXmlMessageSplitterTestCase extends AbstractMuleTestCase
{
    private OutboundEndpoint endpoint1;
    private OutboundEndpoint endpoint2;
    private OutboundEndpoint endpoint3;
    private FilteringXmlMessageSplitter xmlSplitter;

    // @Override
    protected void doSetUp() throws Exception
    {
        // setup endpoints
        endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");

        // setup splitter
        xmlSplitter = new FilteringXmlMessageSplitter();
        xmlSplitter.setValidateSchema(true);
        xmlSplitter.setExternalSchemaLocation("purchase-order.xsd");

        // The xml document declares a default namespace, thus
        // we need to workaround it by specifying it both in
        // the namespaces and in the splitExpression
        Map namespaces = new HashMap();
        namespaces.put("e", "http://www.example.com");
        xmlSplitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        xmlSplitter.setNamespaces(namespaces);
        xmlSplitter.addEndpoint(endpoint1);
        xmlSplitter.addEndpoint(endpoint2);
        xmlSplitter.addEndpoint(endpoint3);
    }

    public void testStringPayloadXmlMessageSplitter() throws Exception
    {
        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    public void testStringPayloadXmlMessageSplitterWithoutXsd() throws Exception
    {
        xmlSplitter.setExternalSchemaLocation(null);
        xmlSplitter.setValidateSchema(false);
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
        xmlSplitter.setEnableCorrelation(AbstractOutboundRouter.ENABLE_CORRELATION_NEVER);
        internalTestSuccessfulXmlSplitter(payload.getBytes());
    }

    private void internalTestSuccessfulXmlSplitter(Object payload) throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        MuleMessage message = new DefaultMuleMessage(payload);

        assertTrue(xmlSplitter.isMatch(message));
        final ItemNodeConstraint itemNodeConstraint = new ItemNodeConstraint();
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint1)));
        xmlSplitter.route(message, (MuleSession)session.proxy(), false);
        session.verify();

        message = new DefaultMuleMessage(payload);

        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint1)), message);
        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint1)), message);
        MuleMessage result = xmlSplitter.route(message, (MuleSession)session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }

    public void testXsdNotFoundThrowsException() throws Exception
    {
        final String invalidSchemaLocation = "non-existent.xsd";
        Mock session = MuleTestUtils.getMockSession();

        FilteringXmlMessageSplitter splitter = new FilteringXmlMessageSplitter();
        splitter.setValidateSchema(true);
        splitter.setExternalSchemaLocation(invalidSchemaLocation);

        String payload = IOUtils.getResourceAsString("purchase-order.xml", getClass());

        MuleMessage message = new DefaultMuleMessage(payload);

        assertTrue(splitter.isMatch(message));
        try
        {
            splitter.route(message, (MuleSession)session.proxy(), false);
            fail("Should have thrown an exception, because XSD is not found.");
        }
        catch (IllegalArgumentException iaex)
        {
            assertTrue("Wrong exception?", iaex.getMessage().indexOf(
                    "Couldn't find schema at " + invalidSchemaLocation) != -1);
        }
        session.verify();
    }

    public void testUnsupportedTypePayloadIsIgnored() throws Exception
    {
        Exception unsupportedPayload = new Exception();

        Mock session = MuleTestUtils.getMockSession();

        MuleMessage message = new DefaultMuleMessage(unsupportedPayload);

        assertTrue(xmlSplitter.isMatch(message));
        xmlSplitter.route(message, (MuleSession)session.proxy(), false);
        session.verify();

        message = new DefaultMuleMessage(unsupportedPayload);

        MuleMessage result = xmlSplitter.route(message, (MuleSession)session.proxy(), true);
        assertNull(result);
        session.verify();
    }

    public void testInvalidXmlPayloadThrowsException() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        FilteringXmlMessageSplitter splitter = new FilteringXmlMessageSplitter();

        MuleMessage message = new DefaultMuleMessage("This is not XML.");

        try
        {
            splitter.route(message, (MuleSession)session.proxy(), false);
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
            final MuleMessage message = (MuleMessage)o;
            final Object payload = message.getPayload();
            assertTrue("Wrong class type for node.", payload instanceof Document);

            // MULE-2963
            if (xmlSplitter.enableCorrelation == AbstractOutboundRouter.ENABLE_CORRELATION_NEVER)
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

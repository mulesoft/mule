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
package org.mule.test.routing.outbound;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.outbound.FilteringXmlMessageSplitter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.util.FileUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 * @version $Revision$
 */

public class FilteringXmlMessageSplitterTestCase extends AbstractMuleTestCase
{
    private UMOEndpoint endpoint1;
    private UMOEndpoint endpoint2;
    private UMOEndpoint endpoint3;
    private FilteringXmlMessageSplitter xmlSplitter;

    protected void doSetUp() throws Exception
    {
        // setup endpoints
        endpoint1 = getTestEndpoint("Test1Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://endpointUri.1"));
        endpoint2 = getTestEndpoint("Test2Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint2.setEndpointURI(new MuleEndpointURI("test://endpointUri.2"));
        endpoint3 = getTestEndpoint("Test3Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint3.setEndpointURI(new MuleEndpointURI("test://endpointUri.3"));

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
        String payload = FileUtils.loadResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload);
    }

    public void testDom4JDocumentPayloadXmlMessageSplitter() throws Exception
    {
        String payload = FileUtils.loadResourceAsString("purchase-order.xml", getClass());
        Document doc = DocumentHelper.parseText(payload);
        internalTestSuccessfulXmlSplitter(doc);
    }

    public void testByteArrayPayloadXmlMessageSplitter() throws Exception
    {
        String payload = FileUtils.loadResourceAsString("purchase-order.xml", getClass());
        internalTestSuccessfulXmlSplitter(payload.getBytes());
    }


    private void internalTestSuccessfulXmlSplitter(Object payload) throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        UMOMessage message = new MuleMessage(payload);

        assertTrue(xmlSplitter.isMatch(message));
        final ItemNodeConstraint itemNodeConstraint = new ItemNodeConstraint();
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint1)));
        xmlSplitter.route(message, (UMOSession) session.proxy(), false);
        session.verify();

        message = new MuleMessage(payload);

        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint1)), message);
        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint1)), message);
        UMOMessage result = xmlSplitter.route(message, (UMOSession) session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }


    public void testXsdNotFoundThrowsException() throws Exception
    {
        final String invalidSchemaLocation = "non-existent.xsd";
        Mock session = MuleTestUtils.getMockSession();

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://endpointUri.1"));

        FilteringXmlMessageSplitter splitter = new FilteringXmlMessageSplitter();
        splitter.setValidateSchema(true);
        splitter.setExternalSchemaLocation(invalidSchemaLocation);

        String payload = FileUtils.loadResourceAsString("purchase-order.xml", getClass());

        UMOMessage message = new MuleMessage(payload);

        assertTrue(splitter.isMatch(message));
        try {
            splitter.route(message, (UMOSession) session.proxy(), false);
            fail("Should have thrown an exception, because XSD is not found.");
        } catch (IllegalArgumentException iaex) {
            assertTrue("Wrong exception?",
                    iaex.getMessage().indexOf("Couldn't find schema at " + invalidSchemaLocation) != -1);
        }
        session.verify();
    }


    public void testUnsupportedTypePayloadIsIgnored() throws Exception
    {
        Exception unsupportedPayload = new Exception();

        Mock session = MuleTestUtils.getMockSession();

        UMOMessage message = new MuleMessage(unsupportedPayload);

        assertTrue(xmlSplitter.isMatch(message));
        xmlSplitter.route(message, (UMOSession) session.proxy(), false);
        session.verify();

        message = new MuleMessage(unsupportedPayload);

        UMOMessage result = xmlSplitter.route(message, (UMOSession) session.proxy(), true);
        assertNull(result);
        session.verify();
    }


    public void testInvalidXmlPayloadThrowsException() throws Exception {
        Mock session = MuleTestUtils.getMockSession();

        FilteringXmlMessageSplitter splitter = new FilteringXmlMessageSplitter();

        UMOMessage message = new MuleMessage("This is not XML.");

        try {
            splitter.route(message, (UMOSession) session.proxy(), false);
            fail("No exception thrown.");
        } catch (IllegalArgumentException iaex) {
            assertTrue("Wrong exception message.",
                       iaex.getMessage().startsWith("Failed to initialise the payload: "));
        }

    }

    private class ItemNodeConstraint implements Constraint
    {
        public boolean eval(Object o)
        {
            final UMOMessage message = (UMOMessage) o;
            final Object payload = message.getPayload();
            assertTrue("Wrong class type for node.",  payload instanceof Document);

            Document node = (Document) payload;

            final String partNumber = node.getRootElement().attributeValue("partNum");


            return "872-AA".equals(partNumber) || "926-AA".equals(partNumber);
        }
    }
}

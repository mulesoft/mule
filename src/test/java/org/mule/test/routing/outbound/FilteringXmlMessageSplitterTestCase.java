/*
 * $Header$
 * $Revision$
 * $Date$
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
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.outbound.FilteringXmlMessageSplitter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.util.Utility;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 * @version $Revision$
 */

public class FilteringXmlMessageSplitterTestCase extends AbstractMuleTestCase
{
    public void testXmlMessageSplitter() throws Exception
    {
        Mock session = getMockSession();

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://endpointUri.1"));
        UMOEndpoint endpoint2 = getTestEndpoint("Test2Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint2.setEndpointURI(new MuleEndpointURI("test://endpointUri.2"));
        UMOEndpoint endpoint3 = getTestEndpoint("Test3Endpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint3.setEndpointURI(new MuleEndpointURI("test://endpointUri.3"));

        FilteringXmlMessageSplitter splitter = new FilteringXmlMessageSplitter();
        splitter.setValidateSchema(true);
        splitter.setExternalSchemaLocation("purchase-order.xsd");

        // The xml document declares a default namespace, thus
        // we need to workaround it by specifying it both in
        // the namespaces and in the splitExpression
        Map namespaces = new HashMap();
        namespaces.put("e", "http://www.example.com");
        splitter.setSplitExpression("/e:purchaseOrder/e:items/e:item");
        splitter.setNamespaces(namespaces);
        splitter.addEndpoint(endpoint1);
        splitter.addEndpoint(endpoint2);
        splitter.addEndpoint(endpoint3);

        String payload = Utility.loadResourceAsString("purchase-order.xml", getClass());

        UMOMessage message = new MuleMessage(payload);

        assertTrue(splitter.isMatch(message));
        final ItemNodeConstraint itemNodeConstraint = new ItemNodeConstraint();
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(itemNodeConstraint, C.eq(endpoint1)));
        splitter.route(message, (UMOSession) session.proxy(), false);
        session.verify();

        message = new MuleMessage(payload);

        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint1)), message);
        session.expectAndReturn("sendEvent", C.args(itemNodeConstraint, C.eq(endpoint1)), message);
        UMOMessage result = splitter.route(message, (UMOSession) session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }

    public void testInvalidPayloadThrowsException() throws Exception {
        Mock session = getMockSession();

        FilteringXmlMessageSplitter splitter = new FilteringXmlMessageSplitter();

        UMOMessage message = new MuleMessage("This is not XML.");

        try {
            splitter.route(message, (UMOSession) session.proxy(), false);
            fail("No exception thrown.");
        } catch (IllegalArgumentException iaex) {
            assertEquals("Failed to initialise the payload.", iaex.getMessage());
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

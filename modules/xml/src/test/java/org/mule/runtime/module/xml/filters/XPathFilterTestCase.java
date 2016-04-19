/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.module.xml.xpath.SaxonXpathEvaluator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Ryan Heaton
 */
@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class XPathFilterTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    private SaxonXpathEvaluator xpathManager;

    private XPathFilter filter;

    @Before
    public void before() throws Exception {
        xpathManager = new SaxonXpathEvaluator();
        when(muleContext.getRegistry()).thenReturn(mock(MuleRegistry.class, RETURNS_DEFAULTS));


        filter = new XPathFilter();
        filter.setMuleContext(muleContext);
        filter.setXpathEvaluator(xpathManager);
    }

    /**
     * tests accepting the mule message.
     */
    @Test
    public void testAcceptMessage() throws Exception
    {
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        DefaultMuleMessage message = new DefaultMuleMessage(null, muleContext);
        XPathFilter filter = new XPathFilter()
        {
            @Override
            public Node toDOMNode(Object src) throws Exception
            {
                return document;
            }

            @Override
            protected boolean accept(Node node)
            {
                return node == document;
            }
        };

        assertFalse("shouldn't accept a message if no payload is set.", filter.accept(message));
        message.setPayload(new Object());
        filter.setPattern("/some/pattern = null");
        assertTrue(filter.accept(message));
        assertEquals("null", filter.getExpectedValue());
        assertEquals("/some/pattern", filter.getPattern().trim());
        assertSame(document, message.getPayload());
        message.setPayload(new Object());
        filter.setExpectedValue(null);
        assertTrue(filter.accept(message));
        assertEquals("true", filter.getExpectedValue());
        assertEquals("/some/pattern", filter.getPattern().trim());
        assertSame(document, message.getPayload());
    }

    /**
     * tests accepting a node.
     */
    @Test
    public void testAcceptNode() throws Exception
    {
        InputStream testXml = getClass().getResourceAsStream("/test.xml");
        assertNotNull(testXml);

        filter.setPattern("/some/unknown/path");
        filter.setExpectedValue("bogus");
        filter.initialise();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document document = builderFactory.newDocumentBuilder().parse(testXml);
        assertFalse("shouldn't have accepted a null evaluation when expected value isn't null.",
            filter.accept(document));
        filter.setExpectedValue("null");
        assertTrue(filter.accept(document));
        filter.setPattern("test/some/in");
        assertFalse(filter.accept(document));
        filter.setExpectedValue("another");
        assertTrue(filter.accept(document));
    }

    /**
     * tests accepting a node.
     */
    @Test
    public void testAcceptSoapNode() throws Exception
    {
        InputStream soapEnvelope = getClass().getResourceAsStream("/request.xml");
        assertNotNull(soapEnvelope);

        filter.setPattern("/soap:Envelope/soap:Body/mule:echo/mule:echo");
        filter.setExpectedValue("Hello!");
        HashMap<String, String> prefix2Namespace = new HashMap<String, String>();
        prefix2Namespace.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        prefix2Namespace.put("mule", "http://simple.component.mule.org/");
        filter.setNamespaces(prefix2Namespace);
        filter.initialise();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document envDoc = builderFactory.newDocumentBuilder().parse(soapEnvelope);
        assertTrue(filter.accept(envDoc));
    }

}

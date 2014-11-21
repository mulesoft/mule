/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.xml.util.properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.registry.RegistrationException;
import org.mule.module.xml.expression.JXPathExpressionEvaluator;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("deprecation") // we're testing a deprecated class
public class JXPathExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private static final String XML_USING_UNDEFINED_NAMESPACE = "<root " +
                                                                "xmlns:z=\"http://www.w3schools.com/furniture\">" +
                                                                "<z:table>" +
                                                                "<z:name>African Coffee Table</z:name>" +
                                                                "<z:width>80</z:width>" +
                                                                "<z:length>120</z:length>" +
                                                                "</z:table>" +
                                                                "</root>";

    @Test
    public void testBean()
    {
        Apple apple = new Apple();
        apple.wash();
        FruitBowl payload = new FruitBowl(apple, new Banana());
        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);

        JXPathExpressionEvaluator e = new JXPathExpressionEvaluator();
        Object value = e.evaluate("apple/washed", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue(((Boolean) value).booleanValue());

        value = e.evaluate("bar", msg);
        assertNull(value);
    }

    @Test
    public void testXmlContainingNoNameSpaces() throws MalformedURLException
    {
        String payload = "<?xml version=\"1.0\" ?>" +
                         "<address>" +
                         "<street>Orchard Road</street>" +
                         "</address>";

        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);

        JXPathExpressionEvaluator e = new JXPathExpressionEvaluator();
        Object value = e.evaluate("/address/street", msg);

        assertTrue(value instanceof String);
        assertEquals("Orchard Road", value);
    }

    @Test
    public void testXmlIfNameSpaceIsDefinedWithTheSamePrefix() throws MalformedURLException
    {
        String payload = "<root " +
                         "xmlns:f=\"http://www.w3schools.com/furniture\">" +
                         "<f:table>" +
                         "<f:name>African Coffee Table</f:name>" +
                         "<f:width>80</f:width>" +
                         "<f:length>120</f:length>" +
                         "</f:table>" +
                         "</root>";

        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);

        JXPathExpressionEvaluator e = new JXPathExpressionEvaluator();
        Object value = e.evaluate("//f:table/f:name", msg);

        assertTrue(value instanceof String);
        assertEquals("African Coffee Table", value);
    }

    @Test
    public void testXmlWithInvalidNameSpaceIfNameSpaceAliasIsUndefined() throws MalformedURLException
    {
        final String xpathExpression = "//f:table/f:name";

        DefaultMuleMessage msg = new DefaultMuleMessage(XML_USING_UNDEFINED_NAMESPACE, muleContext);

        JXPathExpressionEvaluator e = new JXPathExpressionEvaluator();
        Object value = e.evaluate(xpathExpression, msg);
        assertNull(value);
    }

    @Test
    public void testXmlWithInvalidNameSpaceIfNameSpaceAliasIsDefined() throws MalformedURLException, RegistrationException
    {
        NamespaceManager namespaceManager = new NamespaceManager();
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("f", "http://www.w3schools.com/furniture");
        namespaceManager.setNamespaces(namespaces);
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_MULE_NAMESPACE_MANAGER, namespaceManager);

        DefaultMuleMessage msg = new DefaultMuleMessage(XML_USING_UNDEFINED_NAMESPACE, muleContext);

        JXPathExpressionEvaluator e = new JXPathExpressionEvaluator();
        e.setMuleContext(muleContext);

        Object value = e.evaluate("//f:table/f:name", msg);
        assertTrue(value instanceof String);
        assertEquals("African Coffee Table", value);
    }

    @Test
    public void assertNoXXEVulnerable() throws Exception
    {
        String xxe = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                   "<!DOCTYPE spi_doc_type[ <!ENTITY spi_entity_ref SYSTEM 'file:%s'>]>\n" +
                                   "<root>\n" +
                                   "<elem>&spi_entity_ref;</elem>\n" +
                                   "<something/>\n" +
                                   "</root>", IOUtils.getResourceAsUrl("xxe-passwd.txt", this.getClass()).getPath());

        JXPathExpressionEvaluator evaluator = new JXPathExpressionEvaluator();
        evaluator.setMuleContext(muleContext);
        Object value = evaluator.evaluate(".", getTestEvent(xxe).getMessage());
        assertThat(value, instanceOf(String.class));
        assertThat(StringUtils.isBlank((String) value), is(true));
    }
}

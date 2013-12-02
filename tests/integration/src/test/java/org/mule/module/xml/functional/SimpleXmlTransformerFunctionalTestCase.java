/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Ignore;
import org.junit.Test;

public class SimpleXmlTransformerFunctionalTestCase extends FunctionalTestCase
{
    public static final String SERIALIZED = "<org.mule.module.xml.functional.SimpleXmlTransformerFunctionalTestCase_-Parent>\n" +
            "  <child>\n" +
            "    <name>theChild</name>\n" +
            "  </child>\n" +
            "</org.mule.module.xml.functional.SimpleXmlTransformerFunctionalTestCase_-Parent>";


    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/simple-xml-transformer-functional-test-flow.xml";
    }

    @Ignore("flaky test")
    @Test
    public void testXmlOut() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("xml-in", SERIALIZED, null);
        Parent parent = (Parent) request(client, "xml-object-out", Parent.class);
        assertNotNull(parent);
        assertNotNull(parent.getChild());
        assertEquals("theChild", parent.getChild().getName());
    }

    @Test
    public void testObjectXmlOut() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("object-in", new Parent(new Child("theChild")), null);
        String xml = (String) request(client, "object-xml-out", String.class);
        XMLAssert.assertXMLEqual(SERIALIZED, xml);
    }

    protected Object request(MuleClient client, String endpoint, Class<?> clazz) throws MuleException
    {
        MuleMessage message = client.request(endpoint, 3000);
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload().getClass().getName(), clazz.isAssignableFrom(message.getPayload().getClass()));
        return message.getPayload();
    }

    public static class Parent
    {
        private Child child;

        public Parent()
        {
            this(null);
        }

        public Parent(Child child)
        {
            setChild(child);
        }

        public Child getChild()
        {
            return child;
        }

        public void setChild(Child child)
        {
            this.child = child;
        }
    }

    public static class Child
    {
        private String name;

        public Child()
        {
            this(null);
        }

        public Child(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }
}

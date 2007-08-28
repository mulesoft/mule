/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import org.mule.extras.client.MuleClient;
import org.mule.modules.xml.functional.AbstractXmlFunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;


/**
 * This is a simplified version of {@link org.mule.modules.xml.functional.XmlTransformerFunctionalTestCase}
 * The {@link #testObjectXmlOut()} method hangs intermittently.
 */
public class MulticastRouterMule2136TestCase extends AbstractXmlFunctionalTestCase
{

    public static final int TEST_COUNT = 10000;
    public static final String SERIALIZED = "<org.mule.issues.MulticastRouterMule2136TestCase_-Parent>\n" +
                    "  <child/>\n" +
                    "</org.mule.issues.MulticastRouterMule2136TestCase_-Parent>";

    protected String getConfigResources()
    {
        return "issues/multicast-router-mule-2136-test.xml";
    }

    protected MuleClient sendObject() throws UMOException
    {
        MuleClient client = new MuleClient();
        client.dispatch("object-in", new Parent(new Child()), null);
        return client;
    }

    public void testObjectOut() throws UMOException
    {
        receive(sendObject(), "object-out", Parent.class);
    }

    public void testObjectXmlOut() throws UMOException
    {
        String xml = (String) receive(sendObject(), "object-xml-out", String.class);
        assertEquals(SERIALIZED, xml);
    }

    public void testXmlObjectOut() throws UMOException
    {
        receive(sendObject(), "xml-object-out", Parent.class);
    }

    public void testStress() throws UMOException
    {
        int tenth = TEST_COUNT / 10;
        for (int i = 0; i < TEST_COUNT; i++)
        {
            testObjectXmlOut();
            if (i % tenth == 0)
            {
                logger.info("Iteration " + i);
            }
        }
    }


    protected Object receive(MuleClient client, String endpoint, Class clazz) throws UMOException
    {
        UMOMessage message = client.receive(endpoint, TIMEOUT);
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

    }

}
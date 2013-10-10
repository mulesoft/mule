/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.xml.functional.AbstractXmlFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This is a simplified version of
 * {@link org.mule.module.xml.functional.XmlTransformerFunctionalTestCase} The
 * {@link #testObjectXmlOut()} method hangs intermittently.
 */
public class MulticastRouterMule2136TestCase extends AbstractXmlFunctionalTestCase
{
    public static final int TEST_COUNT = 2000; // cut down from 10k messages, since
                                               // it seems a little much for the
                                               // continuous build
    public static final String SERIALIZED = "<org.mule.issues.MulticastRouterMule2136TestCase_-Parent>\n"
                                            + "  <child/>\n"
                                            + "</org.mule.issues.MulticastRouterMule2136TestCase_-Parent>";

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/issues/multicast-router-mule-2136-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/issues/multicast-router-mule-2136-test-flow.xml"}
        });
    }

    public MulticastRouterMule2136TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected MuleClient sendObject() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("object-in", new Parent(new Child()), null);
        return client;
    }

    @Test
    public void testObjectOut() throws MuleException, InterruptedException
    {
        request(sendObject(), "object-out", Parent.class);
        // wait a while, otherwise we pull down everything while it is still running
        Thread.sleep(3000);
    }

    @Test
    public void testObjectXmlOut() throws MuleException
    {
        String xml = (String) request(sendObject(), "object-xml-out", String.class);
        assertEquals(SERIALIZED, xml);
    }

    @Test
    public void testXmlObjectOut() throws MuleException
    {
        request(sendObject(), "xml-object-out", Parent.class);
    }

    @Test
    public void testStress() throws MuleException
    {
        int tenth = TEST_COUNT / 10;
        for (int i = 0; i < TEST_COUNT; i++)
        {
            testObjectXmlOut();

            // Pull result from "xml-object-out" endpoint as queuing is enabled and
            // otherwise we get
            // OutOfMemoryExceptions during stress tests when these results build up
            // in queue.
            request(new MuleClient(muleContext), "xml-object-out", Parent.class);

            if (i % tenth == 0)
            {
                logger.info("Iteration " + i);
            }
        }
    }

    protected Object request(MuleClient client, String endpoint, Class<?> clazz) throws MuleException
    {
        MuleMessage message = client.request(endpoint, TIMEOUT * 2);
        assertNotNull(message);
        assertNotNull(message.getPayload());

        Class<?> payloadClass = message.getPayload().getClass();
        String assertionMessage = String.format("expected payload of type %1s but was %2s", clazz.getName(),
            payloadClass);
        assertTrue(assertionMessage, clazz.isAssignableFrom(payloadClass));
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
        // nothing here
    }
}

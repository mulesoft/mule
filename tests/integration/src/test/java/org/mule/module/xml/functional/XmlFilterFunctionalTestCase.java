/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public class XmlFilterFunctionalTestCase extends AbstractXmlFunctionalTestCase
{
    public static final int MAX_COUNT = 100;
    public static final String STRING_MESSAGE = "Hello world";
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/xml/xml-filter-functional-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/module/xml/xml-filter-functional-test-flow.xml"}});
    }

    public XmlFilterFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testNotXml() throws Exception
    {
        logger.debug("not xml");
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("in", STRING_MESSAGE, null);
        MuleMessage response = client.request("notxml", TIMEOUT);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals(STRING_MESSAGE, response.getPayloadAsString());
    }

    @Test
    public void testOther() throws Exception
    {
        logger.debug("other");
        doTestXml("other", getResourceAsString("org/mule/issues/many-sends-mule-1758-test-service.xml"));
    }

    @Test
    public void testSelf() throws Exception
    {
        logger.debug("self");
        doTestXml("self", getConfigAsString());
    }

    public void doTestXml(String endpoint, String xml) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("in", xml, null);
        MuleMessage response = client.request(endpoint, TIMEOUT * 2);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals(xml, response.getPayloadAsString());
    }

    @Test
    public void testMany() throws Exception
    {
        Random random = new Random();
        for (int i = 0; i < MAX_COUNT; ++i)
        {
            switch (random.nextInt(3))
            {
                case 0 :
                    testNotXml();
                    break;
                case 1 :
                    testOther();
                    break;
                case 2 :
                    testSelf();
                    break;
                default :
                    throw new IllegalStateException("Bad case");
            }
        }
    }
}

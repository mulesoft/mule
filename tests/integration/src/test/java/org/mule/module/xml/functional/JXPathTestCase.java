/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JXPathTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "jxpath-config-service.xml"},
            {ConfigVariant.FLOW, "jxpath-config-flow.xml"}});
    }

    public JXPathTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSetMessagePropertyFromXmlWithNamespacesDefinedWithSamePrefix() throws Exception
    {
        String xml = "<root " + "xmlns:h=\"http://www.w3.org/TR/html4/\" "
                     + "xmlns:f=\"http://www.w3schools.com/furniture\">" +

                     "<h:table>" + "<h:tr>" + "<h:td>Apples</h:td>" + "<h:td>Bananas</h:td>" + "</h:tr>"
                     + "</h:table>" +

                     "<f:table>" + "<f:name>African Coffee Table</f:name>" + "<f:width>80</f:width>"
                     + "<f:length>120</f:length>" + "</f:table>" +

                     "</root>";

        doTest(xml);
    }

    @Test
    public void testSetMessagePropertyFromXmlWithNamespacesDefinedWithDifferentPrefix() throws Exception
    {
        String xml = "<root " + "xmlns:h=\"http://www.w3.org/TR/html4/\" "
                     + "xmlns:z=\"http://www.w3schools.com/furniture\">" +

                     "<h:table>" + "<h:tr>" + "<h:td>Apples</h:td>" + "<h:td>Bananas</h:td>" + "</h:tr>"
                     + "</h:table>" +

                     "<z:table>" + "<z:name>African Coffee Table</z:name>" + "<z:width>80</z:width>"
                     + "<z:length>120</z:length>" + "</z:table>" +

                     "</root>";

        doTest(xml);
    }

    private void doTest(String xml) throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in", xml, null);
        assertNotNull(response);
        assertNotNull(response.getInboundProperty("nameProperty"));
        assertEquals("African Coffee Table", response.getInboundProperty("nameProperty"));
    }
}

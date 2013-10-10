/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.xml;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.IOUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

//START SNIPPET: test-code
public class XSLTWikiDocsTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/xml/xslt-functional-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/xml/xslt-functional-test-flow.xml"}
        });
    }

    public XSLTWikiDocsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testMessageTransform() throws Exception
    {
        //We're using Xml Unit to compare results
        //Ignore whitespace and comments
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);

        //Read in src and result data
        String srcData = IOUtils.getResourceAsString(
                "org/mule/test/integration/xml/cd-catalog.xml", getClass());
        String resultData = IOUtils.getResourceAsString(
                "org/mule/test/integration/xml/cd-catalog-result-with-params.xml", getClass());

        //Create a new Mule Client
        MuleClient client = new MuleClient(muleContext);

        //These are the message roperties that will get passed into the XQuery context
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("ListTitle", "MyList");
        props.put("ListRating", new Integer(6));

        //Invoke the service
        MuleMessage message = client.send("vm://test.in", srcData, props);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        //Compare results

        assertTrue(XMLUnit.compareXML(message.getPayloadAsString(), resultData).similar());

    }
}
//END SNIPPET: test-code

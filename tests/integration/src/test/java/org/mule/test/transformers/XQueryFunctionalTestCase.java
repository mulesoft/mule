/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

public class XQueryFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/xml/xquery-functional-test.xml";
    }

    @Test
    public void testMessageTransform() throws Exception
    {
        //We're using Xml Unit to compare results
        //Ignore whitespace and comments
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);

        //Read in src and result data
        String srcData = IOUtils.getResourceAsString("org/mule/test/integration/xml/cd-catalog.xml", getClass());
        String resultData = IOUtils.getResourceAsString("org/mule/test/integration/xml/cd-catalog-result-with-params.xml", getClass());

        //Create a new Mule Client
        MuleClient client = muleContext.getClient();

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

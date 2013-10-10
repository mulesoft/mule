/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class XsltWithParamsTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/xml/xml-namespace-test.xml";
    }

    @Test
    public void testTransformWithParameter() throws Exception
    {
        Transformer trans = muleContext.getRegistry().lookupTransformer("test1");
        assertNotNull(trans);
        MuleMessage message = new DefaultMuleMessage("<testing/>", muleContext);
        message.setOutboundProperty("Welcome", "hello");
        Object result = trans.transform(message);
        assertNotNull(result);
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><echo-value xmlns=\"http://test.com\">hello</echo-value>", result);
    }
}

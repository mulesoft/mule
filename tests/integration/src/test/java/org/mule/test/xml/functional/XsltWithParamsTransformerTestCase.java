/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.functional;

import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.junit4.rule.ForceXalanTransformerFactory;
import org.mule.tck.junit4.rule.SystemProperty;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("ArtifactClassloaderTestRunner org.apache.xml.serializer error")
public class XsltWithParamsTransformerTestCase extends AbstractIntegrationTestCase
{
    @Rule
    public SystemProperty useXalan = new ForceXalanTransformerFactory();

    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/xml-namespace-test.xml";
    }

    @Test
    public void testTransformWithParameter() throws Exception
    {
        Transformer trans = muleContext.getRegistry().lookupTransformer("test1");
        assertNotNull(trans);
        MuleMessage message = MuleMessage.builder().payload("<testing/>").addOutboundProperty("Welcome", "hello").build();
        Object result = trans.transform(message);
        assertNotNull(result);
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><echo-value xmlns=\"http://test.com\">hello</echo-value>", result);
    }
}

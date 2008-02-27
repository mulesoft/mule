/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.functional;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.transformer.Transformer;
import org.mule.tck.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;

public class XsltWithParamsTransformerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/module/xml/xml-namespace-test.xml";
    }

    protected void doSetUp() throws Exception
    {
        MuleEvent event = getTestEvent("<testing/>");
        //We need a current event to pull the parameter from
        event.getMessage().setProperty("Welcome", "hello");
        RequestContext.setEvent(event);
    }

    public void testTransformWithParameter() throws Exception
    {
        Transformer trans = muleContext.getRegistry().lookupTransformer("test1");
        assertNotNull(trans);
        Object result = trans.transform("<testing/>");
        assertNotNull(result);
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><echo-value xmlns=\"http://test.com\">hello</echo-value>", result);
    }
}
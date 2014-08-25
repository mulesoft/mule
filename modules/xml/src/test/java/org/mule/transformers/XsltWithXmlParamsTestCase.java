/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

public class XsltWithXmlParamsTestCase extends FunctionalTestCase
{

    private static final String EXPECTED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><result><body><just>testing</just></body><fromParam>value element</fromParam></result>";

    @Override
    protected String getConfigFile()
    {
        return "xslt-with-xml-param-config.xml";
    }

    @Test
    public void xmlSourceParam() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("xmlSourceParam");
        MuleEvent event = flow.process(buildEvent(XMLTestUtils.toSource("simple.xml"), XMLTestUtils.toSource("test.xml")));

        assertExpected(event);
    }

    @Test
    public void xmlStringParam() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("xmlStringParam");
        MuleEvent event = flow.process(buildEvent(XMLTestUtils.toString("simple.xml"), XMLTestUtils.toString("test.xml")));

        assertExpected(event);
    }

    private MuleEvent buildEvent(Object payload, Object param) throws Exception
    {
        MuleEvent event = getTestEvent(payload);
        event.setFlowVariable("xml", param);

        return event;
    }

    private void assertExpected(MuleEvent event) throws Exception
    {
        assertThat(XMLUnit.compareXML(event.getMessage().getPayload().toString(), EXPECTED).similar(), is(true));
    }


}

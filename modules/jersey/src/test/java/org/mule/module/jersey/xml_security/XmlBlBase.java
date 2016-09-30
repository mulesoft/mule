/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.xml_security;

import static java.lang.String.format;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Before;
import org.junit.Rule;

/**
 * Test for XML Billion Laughs attack
 * https://en.wikipedia.org/wiki/Billion_laughs
 */
public class XmlBlBase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    protected MuleClient client;
    protected String url;

    protected final String xmlWithEntities =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<!DOCTYPE foo[ \n" +
            "<!ENTITY xxe1 \"0101\"> \n" +
            "]> \n" +
            "<Quote> \n" +
            "<fName>FIRST NAME</fName> \n" +
            "<lName>LAST NAME &xxe1;</lName> \n" +
            "</Quote>";

    @Override
    protected String getConfigFile()
    {
        return "xml-security-config-flow.xml";
    }

    @Before
    public void setUp()
    {
        client = muleContext.getClient();
        url = format("http://localhost:%d/service/customer", port.getNumber());
    }
}

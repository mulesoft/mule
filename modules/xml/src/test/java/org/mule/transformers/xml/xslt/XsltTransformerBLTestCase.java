/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * This test case validates that the XSLT transformer is not vulnerable to
 * Billion Laughs attack when internal entity expansion is disabled
 * <p>
 * <b>EIP Reference:</b> <a
 * href="https://en.wikipedia.org/wiki/Billion_laughs"<a/>
 * </p>
 */
public class XsltTransformerBLTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "xslt-bl-config.xml";
    }

    protected String makeInput()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<!DOCTYPE foo [<!ENTITY lol1 \"01\">" +
                 "<!ENTITY lol2 \"&lol1;&lol1;\">" +
                 "<!ENTITY lol3 \"&lol2;&lol2;\">" +
                 "<!ENTITY lol4 \"&lol3;&lol3;\">" +
               "]> \n" +
               "<entityName>Hello123456890 &lol4;&lol4;&lol4;</entityName>";
    }

    @Test
    public void enabled() throws Exception
    {
        String input = makeInput();
        Object payload = input.getBytes();
        String output = (String) runFlow("flowBLEnabled", payload).getMessage().getPayload();
        assertThat(output, containsString("010101010101010101010101010101010101010101010101"));
    }

    @Test(expected = TransformerMessagingException.class)
    public void disabled() throws Exception
    {
        String input = makeInput();
        Object payload = input.getBytes();
        String output = (String) runFlow("flowBLDisabled", payload).getMessage().getPayload();
    }
}

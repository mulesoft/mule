/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.el;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.InputStream;

import org.junit.Test;

public class XPath3NamespaceTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "xpath-namespace-config.xml";
    }

    @Test
    public void xpathWithNamespace() throws Exception
    {
        InputStream soapEnvelope = getClass().getResourceAsStream("/request.xml");
        String result = runFlow("xpathWithNamespace", soapEnvelope).getMessage().getPayloadAsString();
        assertThat(result, equalTo("Hello!"));
    }
}

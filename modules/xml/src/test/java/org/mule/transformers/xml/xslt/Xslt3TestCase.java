/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import static org.junit.matchers.JUnitMatchers.containsString;

import org.mule.api.MuleException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Xslt3TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "xsl/xslt3-config.xml";
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected void doSetUp() throws Exception
    {
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void nullParameter() throws Exception
    {
        expectedException.expect(MuleException.class);
        expectedException.expectMessage(containsString("null"));
        runFlow("nullParam", "<parameter/>").getMessage().getPayloadAsString();
    }
}

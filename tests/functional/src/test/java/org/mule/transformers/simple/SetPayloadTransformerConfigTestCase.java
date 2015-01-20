/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.SetPayloadTransformer;

import org.junit.Test;

public class SetPayloadTransformerConfigTestCase extends FunctionalTestCase
{

    private static final String FOO = "foo";

    @Override
    protected String getConfigFile()
    {
        return "set-payload-transformer-test-config.xml";
    }

    @Test
    public void text() throws Exception
    {
        SetPayloadTransformer trans = (SetPayloadTransformer) muleContext.getRegistry().lookupTransformer("text");
        assertNotNull(trans);
        trans.initialise();
        assertThat((String) trans.transform("", ""), is(equalTo(FOO)));
    }

    @Test
    public void expression() throws Exception
    {
        SetPayloadTransformer trans = (SetPayloadTransformer) muleContext.getRegistry().lookupTransformer("expression");
        assertNotNull(trans);
        trans.initialise();
        assertThat((String) trans.transform("", ""), is(equalTo(FOO)));
    }

    @Test
    public void embeddedExpressions() throws Exception
    {
        SetPayloadTransformer trans = (SetPayloadTransformer) muleContext.getRegistry().lookupTransformer
                ("embeddedExpressions");
        assertNotNull(trans);
        trans.initialise();
        assertThat((String) trans.transform("", ""), is(equalTo(FOO)));
    }

    @Test
    public void unparsedEmbeddedExpressions() throws Exception
    {
        SetPayloadTransformer trans = (SetPayloadTransformer) muleContext.getRegistry().lookupTransformer
                ("unparsedEmbeddedExpressions");
        assertNotNull(trans);
        trans.initialise();
        assertThat((String) trans.transform("", ""), is(equalTo("#['f']o#['o']")));
    }

}

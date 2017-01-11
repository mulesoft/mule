/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import org.mule.api.transformer.TransformerMessagingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class XsltTransformerBLDisabledTestCase extends XsltTransformerBLTestCase
{

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void disabled() throws Exception
    {
        String input = makeInput();
        Object payload = input.getBytes();

        exception.expect(TransformerMessagingException.class);
        exception.expectMessage("Undeclared general entity");

        String output = (String) runFlow("flow", payload).getMessage().getPayload();
    }

}

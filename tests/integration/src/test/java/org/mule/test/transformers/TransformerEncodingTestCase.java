/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.runtime.core.util.Base64;

import java.io.IOException;

import org.junit.Test;

public class TransformerEncodingTestCase extends FunctionalTestCase
{

    private static final String UTF_16_LE = "UTF-16LE";
    private static final String PAYLOAD = "This a string with swedish characters - \u00E4 \u00D6 \u00E5";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/transformer-encoding-config.xml";
    }

    @Test
    public void encodingFromTransformer() throws Exception
    {
        MuleMessage message = getMuleMessage();
        testEncoding("base64decode", message);
    }

    @Test
    public void encodingFromSetProperty() throws Exception
    {
        MuleMessage message = getMuleMessage();
        testEncoding("base64decode2", message);
    }

    @Test
    public void encodingFromMessage() throws Exception
    {
        MutableMuleMessage message = getMuleMessage();
        message.setOutboundProperty(MULE_ENCODING_PROPERTY, UTF_16_LE);
        testEncoding("base64decode", message);
    }

    private void testEncoding(String flowName, MuleMessage message) throws Exception
    {
        final MuleEvent muleEvent = flowRunner(flowName).withPayload(message).run();

        MuleMessage response = muleEvent.getMessage();
        assertThat(getPayloadAsString(response), is(PAYLOAD));
    }

    private MutableMuleMessage getMuleMessage() throws IOException
    {
        return new DefaultMuleMessage(Base64.encodeBytes(PAYLOAD.getBytes(UTF_16_LE)));
    }

}

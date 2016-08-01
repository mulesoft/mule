/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.Base64;

import java.nio.charset.Charset;

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
        testEncoding("base64decode");
    }

    @Test
    public void encodingFromMessage() throws Exception
    {
        testEncoding("base64decode", UTF_16_LE);
    }

    private void testEncoding(String flowName) throws Exception
    {
        assertPayload(flowRunner(flowName).withPayload(Base64.encodeBytes(PAYLOAD.getBytes(UTF_16_LE))).run());
    }

    private void testEncoding(String flowName, String charset) throws Exception
    {
        assertPayload(flowRunner(flowName).withPayload(Base64.encodeBytes(PAYLOAD.getBytes(UTF_16_LE)))
                                          .withMediaType(MediaType.ANY.withCharset(Charset.forName(charset)))
                                          .run());
    }

    protected void assertPayload(final MuleEvent muleEvent) throws Exception
    {
        assertThat(getPayloadAsString(muleEvent.getMessage()), is(PAYLOAD));
    }
}

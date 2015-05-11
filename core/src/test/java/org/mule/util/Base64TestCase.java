/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.compression.GZIPCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

@SmallTest
public class Base64TestCase extends AbstractMuleTestCase
{

    @Test
    public void decodeWithoutUnzipping() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphabetic(1024);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload.getBytes());
        GZIPCompressorInputStream gzipCompressorInputStream = new GZIPCompressorInputStream(byteArrayInputStream);

        String encoded = Base64.encodeBytes(IOUtils.toByteArray(gzipCompressorInputStream), Base64.DONT_BREAK_LINES);
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(Base64.decodeWithoutUnzipping(encoded)));

        assertThat(IOUtils.toString(gzipInputStream), is(payload));
    }
}

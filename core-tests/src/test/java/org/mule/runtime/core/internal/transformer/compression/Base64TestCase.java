/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.compression;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.util.Base64;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

@SmallTest
public class Base64TestCase extends AbstractMuleTestCase {

  @Test
  public void decodeWithoutUnzipping() throws Exception {
    final String payload = RandomStringUtils.randomAlphabetic(1024);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload.getBytes());
    GZIPCompressorInputStream gzipCompressorInputStream = new GZIPCompressorInputStream(byteArrayInputStream);

    String encoded = Base64.encodeBytes(IOUtils.toByteArray(gzipCompressorInputStream), Base64.DONT_BREAK_LINES);
    GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(Base64.decodeWithoutUnzipping(encoded)));

    assertThat(IOUtils.toString(gzipInputStream), is(payload));
  }
}

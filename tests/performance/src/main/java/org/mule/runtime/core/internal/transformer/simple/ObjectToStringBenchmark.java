/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.RandomStringUtils.random;

import org.mule.AbstractBenchmark;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@OutputTimeUnit(NANOSECONDS)
public class ObjectToStringBenchmark extends AbstractBenchmark {

  private static final Charset charset = defaultCharset();
  private ObjectToString objectToString = new ObjectToString();
  private InputStream inputStream = new StringBufferInputStream(random(20 * 1024 * 1024));

  @Benchmark
  public Object inputStreamToString() throws TransformerException {
    return objectToString.doTransform(inputStream, charset);
  }

  @Benchmark
  public Object inputStreamToStringIoUtilsToString() throws TransformerException {
    return IOUtils.toString(inputStream);
  }

  /*
   * This approach is over twice as fast as using IOUtils.toString or IOUtils.copy with a StringWriter in micro-benchmarks.
   */
  @Benchmark
  public Object inputStreamToStringIOUtilsCopyOutputStream() throws TransformerException, IOException {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    copy(inputStream, byteOut);
    return byteOut.toString(charset.name());
  }

  @Benchmark
  public Object inputStreamToStringIOUtilsCopyWriter() throws TransformerException, IOException {
    StringWriter writer = new StringWriter();
    copy(inputStream, writer, charset);
    return writer.toString();
  }

}

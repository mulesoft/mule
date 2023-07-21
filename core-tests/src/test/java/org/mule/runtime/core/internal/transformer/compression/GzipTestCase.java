/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.compression;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Issue;

public class GzipTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  @Issue("COMPM-10")
  public void compressDeflaterEnded() throws IOException {
    Deflater deflater;
    try (final TestGzipCompressorInputStream gzipCompressorInputStream =
        new TestGzipCompressorInputStream(new ByteArrayInputStream(new byte[] {}))) {
      deflater = gzipCompressorInputStream.getDeflater();
    }

    expected.expectMessage("Deflater has been closed");
    deflater.getBytesRead();
  }

  private static final class TestGzipCompressorInputStream extends GZIPCompressorInputStream {

    private final Deflater deflater;

    private TestGzipCompressorInputStream(InputStream in) {
      super(in);
      this.deflater = this.def;
    }

    public Deflater getDeflater() {
      return deflater;
    }
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Provides a hybrid Writer/OutputStream for sending HTTP response data
 */
public class ResponseWriter extends FilterWriter {

  public static final String CRLF = "\r\n";
  private OutputStream outStream = null;
  private Charset encoding = null;

  public ResponseWriter(final OutputStream outStream) throws UnsupportedEncodingException {
    this(outStream, CRLF, ISO_8859_1);
  }

  public ResponseWriter(final OutputStream outStream, final Charset encoding) throws UnsupportedEncodingException {
    this(outStream, CRLF, encoding);
  }

  public ResponseWriter(final OutputStream outStream, final String lineSeparator, final Charset encoding)
      throws UnsupportedEncodingException {
    super(new BufferedWriter(new OutputStreamWriter(outStream, encoding)));
    this.outStream = outStream;
    this.encoding = encoding;
  }

  public Charset getEncoding() {
    return encoding;
  }

  @Override
  public void close() throws IOException {
    if (outStream != null) {
      super.close();
      outStream = null;
    }
  }

  @Override
  public void flush() throws IOException {
    if (outStream != null) {
      super.flush();
      outStream.flush();
    }
  }

  public void write(byte b) throws IOException {
    super.flush();
    outStream.write(b);
  }

  public void write(byte[] b) throws IOException {
    super.flush();
    outStream.write(b);
  }

  public void write(byte[] b, int off, int len) throws IOException {
    super.flush();
    outStream.write(b, off, len);
  }

  public void print(String s) throws IOException {
    if (s == null) {
      s = "null";
    }
    write(s);
  }

  public void print(int i) throws IOException {
    write(Integer.toString(i));
  }

  public void println(int i) throws IOException {
    write(Integer.toString(i));
    write(CRLF);
  }

  public void println(String s) throws IOException {
    print(s);
    write(CRLF);
  }

  public void println() throws IOException {
    write(CRLF);
  }

}

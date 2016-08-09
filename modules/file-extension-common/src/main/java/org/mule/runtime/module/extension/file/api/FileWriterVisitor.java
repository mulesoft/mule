/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import static org.apache.commons.io.IOUtils.write;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * A {@link FileContentVisitor} which writes the received content into an {@link #outputStream}
 *
 * @since 4.0
 */
public class FileWriterVisitor implements FileContentVisitor {

  private final OutputStream outputStream;
  private final MuleEvent event;
  private final String encoding;

  /**
   * Creates a new instance
   *
   * @param outputStream the stream to write into
   * @param event a {@link MuleEvent} to be used to power the {@link #visit(OutputHandler)} case
   * @param encoding the encoding to use when writing a content of type {@link String}
   */
  public FileWriterVisitor(OutputStream outputStream, MuleEvent event, String encoding) {
    this.outputStream = outputStream;
    this.event = event;
    this.encoding = encoding;
  }

  @Override
  public void visit(String content) throws Exception {
    try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, encoding)) {
      write(content, writer);
      writer.flush();
    }
  }

  @Override
  public void visit(byte content) throws Exception {
    outputStream.write(content);
  }

  @Override
  public void visit(byte[] content) throws Exception {
    write(content, outputStream);
  }

  @Override
  public void visit(OutputHandler handler) throws Exception {
    handler.write((org.mule.runtime.core.api.MuleEvent) event, outputStream);
  }

  @Override
  public void visit(InputStream content) throws Exception {
    IOUtils.copy(content, outputStream);
  }
}

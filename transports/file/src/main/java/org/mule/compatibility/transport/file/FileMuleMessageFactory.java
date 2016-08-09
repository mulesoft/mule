/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_DIRECTORY;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_FILE_SIZE;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_FILE_TIMESTAMP;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_ORIGINAL_FILENAME;
import org.mule.compatibility.core.transport.AbstractMuleMessageFactory;
import org.mule.runtime.core.api.MuleMessage;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.activation.MimetypesFileTypeMap;

/**
 * <code>FileMuleMessageFactory</code> creates a new {@link MuleMessage} with a {@link File} or {@link InputStream} payload. Users
 * can obtain the filename and directory in the properties using <code>FileConnector.PROPERTY_FILENAME</code> and
 * <code>FileConnector.PROPERTY_DIRECTORY</code>.
 */
public class FileMuleMessageFactory extends AbstractMuleMessageFactory {

  private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

  @Override
  protected Class<?>[] getSupportedTransportMessageTypes() {
    return new Class[] {File.class, ReceiverFileInputStream.class};
  }

  @Override
  protected Object extractPayload(Object transportMessage, Charset encoding) throws Exception {
    return transportMessage;
  }

  @Override
  protected void addProperties(MuleMessage.Builder messageBuilder, Object transportMessage) throws Exception {
    super.addProperties(messageBuilder, transportMessage);
    File file = convertToFile(transportMessage);
    setPropertiesFromFile(messageBuilder, file);
  }

  @Override
  protected String getMimeType(Object transportMessage) {
    File file = convertToFile(transportMessage);

    return mimetypesFileTypeMap.getContentType(file.getName().toLowerCase());
  }

  protected File convertToFile(Object transportMessage) {
    File file = null;

    if (transportMessage instanceof File) {
      file = (File) transportMessage;
    } else if (transportMessage instanceof ReceiverFileInputStream) {
      file = ((ReceiverFileInputStream) transportMessage).getCurrentFile();
    }

    return file;
  }

  protected void setPropertiesFromFile(MuleMessage.Builder messageBuilder, File file) {
    messageBuilder.addInboundProperty(PROPERTY_ORIGINAL_FILENAME, file.getName());
    messageBuilder.addInboundProperty(PROPERTY_DIRECTORY, file.getParent());
    messageBuilder.addInboundProperty(PROPERTY_FILE_SIZE, file.length());
    messageBuilder.addInboundProperty(PROPERTY_FILE_TIMESTAMP, file.lastModified());
  }
}

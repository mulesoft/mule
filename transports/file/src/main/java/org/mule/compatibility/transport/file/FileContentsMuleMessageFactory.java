/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <code>FileContentsMuleMessageFactory</code> converts the {@link InputStream}'s content into a <code>byte[]</code> as payload
 * for the {@link MuleMessage}.
 */
public class FileContentsMuleMessageFactory extends FileMuleMessageFactory {

  @Override
  protected Class<?>[] getSupportedTransportMessageTypes() {
    return new Class[] {InputStream.class, File.class};
  }

  @Override
  protected Object extractPayload(Object transportMessage, Charset encoding) throws Exception {
    InputStream inputStream = convertToInputStream(transportMessage);
    byte[] payload = IOUtils.toByteArray(inputStream);
    inputStream.close();
    return payload;
  }

  private InputStream convertToInputStream(Object transportMessage) throws Exception {
    InputStream stream = null;

    if (transportMessage instanceof InputStream) {
      stream = (InputStream) transportMessage;
    } else if (transportMessage instanceof File) {
      stream = new FileInputStream((File) transportMessage);
    }

    return stream;
  }
}

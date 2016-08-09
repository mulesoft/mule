/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_FILENAME;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.compatibility.transport.file.i18n.FileMessages;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.IOUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;

/**
 * <code>FileMessageDispatcher</code> is used to read/write files to the filesystem
 */
public class FileMessageDispatcher extends AbstractMessageDispatcher {

  private final FileConnector connector;

  public FileMessageDispatcher(OutboundEndpoint endpoint) {
    super(endpoint);
    this.connector = (FileConnector) endpoint.getConnector();

    if (endpoint.getProperty("outputAppend") != null) {
      throw new IllegalArgumentException("Configuring 'outputAppend' on a file endpoint is no longer supported. You may configure it on a file connector instead.");
    }
  }

  @Override
  protected void doDispatch(MuleEvent event) throws Exception {
    Object data = event.getMessage().getPayload();
    // Wrap the transformed message before passing it to the filename parser
    MuleMessage.Builder messageBuilder = MuleMessage.builder(event.getMessage()).payload(data);

    FileOutputStream fos = (FileOutputStream) connector.getOutputStream(getEndpoint(), event);
    try {
      if (event.getMessage().getOutboundProperty(PROPERTY_FILENAME) == null) {
        messageBuilder.addOutboundProperty(PROPERTY_FILENAME, event.getMessage().getOutboundProperty(PROPERTY_FILENAME, EMPTY));
      }
      event.setMessage(messageBuilder.build());


      if (data instanceof byte[]) {
        fos.write((byte[]) data);
      } else if (data instanceof String) {
        fos.write(data.toString().getBytes(resolveEncoding(event)));
      } else if (data instanceof OutputHandler) {
        ((OutputHandler) data).write(event, fos);
      } else {
        InputStream is = (InputStream) event.transformMessage(DataType.fromType(InputStream.class));
        IOUtils.copyLarge(is, fos);
        is.close();
      }
    } finally {
      logger.debug("Closing file");
      fos.close();
    }
  }

  /**
   * There is no associated session for a file connector
   *
   * @throws MuleException
   */
  public Object getDelegateSession() throws MuleException {
    return null;
  }

  protected static File getNextFile(String dir, Object filter) throws MuleException {
    File[] files;
    File file = FileUtils.newFile(dir);
    File result = null;
    try {
      if (file.exists()) {
        if (file.isFile()) {
          result = file;
        } else if (file.isDirectory()) {
          if (filter != null) {
            if (filter instanceof FileFilter) {
              files = file.listFiles((FileFilter) filter);
            } else if (filter instanceof FilenameFilter) {
              files = file.listFiles((FilenameFilter) filter);
            } else {
              throw new DefaultMuleException(FileMessages.invalidFilter(filter));
            }
          } else {
            files = file.listFiles();
          }
          if (files.length > 0) {
            result = getFirstFile(files);
          }
        }
      }
      return result;
    } catch (Exception e) {
      throw new DefaultMuleException(FileMessages.errorWhileListingFiles(), e);
    }
  }

  private static File getFirstFile(File[] files) {
    for (File file : files) {
      if (file.isFile()) {
        return file;
      }
    }

    return null;
  }

  @Override
  protected MuleMessage doSend(MuleEvent event) throws Exception {
    doDispatch(event);
    return MuleMessage.builder().nullPayload().build();
  }

  @Override
  protected void doDispose() {
    // no op
  }

  @Override
  protected void doConnect() throws Exception {
    // no op
  }

  @Override
  protected void doDisconnect() throws Exception {
    // no op
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.transport.AbstractMessageRequester;
import org.mule.compatibility.transport.file.i18n.FileMessages;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * <code>FileMessageRequester</code> is used to read/write files to the filesystem
 */
public class FileMessageRequester extends AbstractMessageRequester {

  private final FileConnector fileConnector;

  private String workDir = null;
  private String workFileNamePattern = null;
  private FilenameFilter filenameFilter = null;
  private FileFilter fileFilter = null;

  public FileMessageRequester(InboundEndpoint endpoint) throws MuleException {
    super(endpoint);
    this.fileConnector = (FileConnector) endpoint.getConnector();

    this.workDir = fileConnector.getWorkDirectory();
    this.workFileNamePattern = fileConnector.getWorkFileNamePattern();

    Filter filter = endpoint.getFilter();
    if (filter instanceof FilenameFilter) {
      filenameFilter = (FilenameFilter) filter;
    } else if (filter instanceof FileFilter) {
      fileFilter = (FileFilter) filter;
    } else if (filter != null) {
      throw new CreateException(FileMessages.invalidFileFilter(endpoint.getEndpointURI()), this);
    }
  }

  /**
   * There is no associated session for a file connector
   *
   * @throws org.mule.api.MuleException
   */
  public Object getDelegateSession() throws MuleException {
    return null;
  }

  /**
   * Will attempt to do a receive from a directory, if the endpointUri resolves to a file name the file will be returned,
   * otherwise the first file in the directory according to the filename filter configured on the connector.
   *
   * @param timeout this is ignored when doing a receive on this dispatcher
   * @return a message containing file contents or null if there was notthing to receive
   * @throws Exception
   */
  @Override
  protected MuleMessage doRequest(long timeout) throws Exception {
    File file = FileUtils.newFile(endpoint.getEndpointURI().getAddress());
    File result = null;

    if (file.exists()) {
      if (file.isFile()) {
        result = file;
      } else if (file.isDirectory()) {
        if (fileFilter != null) {
          result = FileMessageDispatcher.getNextFile(endpoint.getEndpointURI().getAddress(), fileFilter);
        } else {
          result = FileMessageDispatcher.getNextFile(endpoint.getEndpointURI().getAddress(), filenameFilter);
        }
      }

      if (result != null) {
        boolean checkFileAge = fileConnector.getCheckFileAge();
        if (checkFileAge) {
          long fileAge = fileConnector.getFileAge();
          long lastMod = result.lastModified();
          long now = System.currentTimeMillis();
          long thisFileAge = now - lastMod;
          if (thisFileAge < fileAge) {
            if (logger.isDebugEnabled()) {
              logger.debug("The file has not aged enough yet, will return nothing for: " + result.getCanonicalPath());
            }
            return null;
          }
        }

        // Don't we need to try to obtain a file lock as we do with receiver
        String originalSourceFileName = result.getName();
        String originalSourceDirectory = result.getParent();

        File workFile = null;
        if (workDir != null) {
          String workFileName = formatUsingFilenameParser(originalSourceFileName, originalSourceDirectory, workFileNamePattern);

          // don't use new File() directly, see MULE-1112
          workFile = FileUtils.newFile(workDir, workFileName);

          fileConnector.move(result, workFile);

          // Now the Work File is the Source file
          result = workFile;
        }

        // set up destination file
        File destinationFile = null;
        String movDir = getMoveDirectory();
        if (movDir != null) {
          String destinationFileName = originalSourceFileName;
          String moveToPattern = getMoveToPattern();
          if (moveToPattern != null) {
            destinationFileName = formatUsingFilenameParser(originalSourceFileName, originalSourceDirectory, moveToPattern);
          }
          // don't use new File() directly, see MULE-1112
          destinationFile = FileUtils.newFile(movDir, destinationFileName);
        }

        MuleMessage returnMessage = null;
        Charset encoding = endpoint.getEncoding();
        try {
          if (fileConnector.isStreaming()) {
            ReceiverFileInputStream receiverStream =
                new ReceiverFileInputStream(result, fileConnector.isAutoDelete(), destinationFile);
            returnMessage = createMuleMessage(receiverStream, encoding);
          } else {
            returnMessage = createMuleMessage(result, encoding);
          }
        } catch (FileNotFoundException e) {
          // we can ignore since we did manage to acquire a lock, but just
          // in case
          logger.error("File being read disappeared!", e);
          return null;
        }

        returnMessage = MuleMessage.builder(returnMessage)
            .addInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, originalSourceFileName)
            .addInboundProperty(FileConnector.PROPERTY_ORIGINAL_DIRECTORY, originalSourceDirectory).build();

        if (!fileConnector.isStreaming()) {
          moveOrDelete(result, destinationFile);
        }

        // If we are streaming no need to move/delete now, that will be
        // done when stream is closed
        return returnMessage;
      }
    }
    return null;
  }

  protected String formatUsingFilenameParser(String originalFileName, String originalDirectory, String pattern) {
    // This isn't nice but is needed as MuleMessage is required to resolve
    // destination file name
    MuleMessage fileParserMessasge =
        MuleMessage.builder().nullPayload().addInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, originalFileName)
            .addInboundProperty(FileConnector.PROPERTY_ORIGINAL_DIRECTORY, originalDirectory).build();

    FlowConstruct flowConstruct = new FlowConstruct() {

      @Override
      public MuleContext getMuleContext() {
        return endpoint.getMuleContext();
      }

      @Override
      public String getName() {
        return "FileMessageRequester";
      }

      @Override
      public LifecycleState getLifecycleState() {
        return null;
      }

      @Override
      public MessagingExceptionHandler getExceptionListener() {
        return null;
      }

      @Override
      public FlowConstructStatistics getStatistics() {
        return null;
      }
    };
    final MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flowConstruct, endpoint.getAddress()))
        .message(fileParserMessasge).flow(flowConstruct).build();

    return fileConnector.getFilenameParser().getFilename(event, pattern);
  }

  private void moveOrDelete(final File sourceFile, File destinationFile) throws DefaultMuleException {

    if (destinationFile != null) {
      // move sourceFile to new destination
      try {
        FileUtils.moveFile(sourceFile, destinationFile);
      } catch (IOException e) {
        throw new DefaultMuleException(FileMessages.failedToMoveFile(sourceFile.getAbsolutePath(),
                                                                     destinationFile.getAbsolutePath()));
      }
    }
    if (fileConnector.isAutoDelete()) {
      // no moveTo directory
      if (destinationFile == null) {
        // delete source
        if (!sourceFile.delete()) {
          throw new DefaultMuleException(FileMessages.failedToDeleteFile(sourceFile));
        }
      }
    }
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

  protected String getMoveDirectory() {
    String moveDirectory = (String) endpoint.getProperty(FileConnector.PROPERTY_MOVE_TO_DIRECTORY);
    if (moveDirectory == null) {
      moveDirectory = fileConnector.getMoveToDirectory();
    }
    return moveDirectory;
  }

  protected String getMoveToPattern() {
    String pattern = (String) endpoint.getProperty(FileConnector.PROPERTY_MOVE_TO_PATTERN);
    if (pattern == null) {
      pattern = fileConnector.getMoveToPattern();
    }
    return pattern;
  }
}

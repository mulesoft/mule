/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_FILENAME;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_ORIGINAL_DIRECTORY;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_ORIGINAL_FILENAME;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_SOURCE_DIRECTORY;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_SOURCE_FILENAME;
import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.message.MuleCompatibilityMessage;
import org.mule.compatibility.core.message.MuleCompatibilityMessageBuilder;
import org.mule.compatibility.core.transport.AbstractPollingMessageReceiver;
import org.mule.compatibility.transport.file.i18n.FileMessages;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEvent.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategy;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.lock.LockFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.apache.commons.collections.comparators.ReverseComparator;

/**
 * <code>FileMessageReceiver</code> is a polling listener that reads files from a directory.
 */

public class FileMessageReceiver extends AbstractPollingMessageReceiver {

  public static final String COMPARATOR_CLASS_NAME_PROPERTY = "comparator";
  public static final String COMPARATOR_REVERSE_ORDER_PROPERTY = "reverseOrder";
  public static final String MULE_TRANSPORT_FILE_SINGLEPOLLINSTANCE = "mule.transport.file.singlepollinstance";

  private static final List<File> NO_FILES = new ArrayList<>();

  private FileConnector fileConnector = null;
  private String readDir = null;
  private String moveDir = null;
  private String workDir = null;
  private File readDirectory = null;
  private File moveDirectory = null;
  private String moveToPattern = null;
  private String workFileNamePattern = null;
  private FilenameFilter filenameFilter = null;
  private FileFilter fileFilter = null;
  private boolean forceSync;
  private LockFactory lockFactory;
  private boolean poolOnPrimaryInstanceOnly;
  private ObjectStore<String> filesBeingProcessingObjectStore;

  public FileMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint, String readDir,
                             String moveDir, String moveToPattern, long frequency)
      throws CreateException {
    super(connector, flowConstruct, endpoint);
    this.fileConnector = (FileConnector) connector;

    setFrequency(frequency);

    this.readDir = readDir;
    this.moveDir = moveDir;
    this.moveToPattern = moveToPattern;
    this.workDir = fileConnector.getWorkDirectory();
    this.workFileNamePattern = fileConnector.getWorkFileNamePattern();

    if (endpoint.getFilter() instanceof FilenameFilter) {
      filenameFilter = (FilenameFilter) endpoint.getFilter();
    } else if (endpoint.getFilter() instanceof FileFilter) {
      fileFilter = (FileFilter) endpoint.getFilter();
    } else if (endpoint.getFilter() != null) {
      throw new CreateException(FileMessages.invalidFileFilter(endpoint.getEndpointURI()), this);
    }

    checkMustForceSync();
  }

  /**
   * If we will be autodeleting File objects, events must be processed synchronously to avoid a race
   */
  protected void checkMustForceSync() throws CreateException {
    boolean connectorIsAutoDelete = false;
    boolean isStreaming = false;
    if (connector instanceof FileConnector) {
      connectorIsAutoDelete = fileConnector.isAutoDelete();
      isStreaming = fileConnector.isStreaming();
    }

    boolean messageFactoryConsumes = (createMuleMessageFactory() instanceof FileContentsMuleMessageFactory);

    forceSync = connectorIsAutoDelete && !messageFactoryConsumes && !isStreaming;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    this.lockFactory = getEndpoint().getMuleContext().getLockFactory();
    boolean synchronousProcessing = false;
    if (getFlowConstruct() instanceof Flow) {
      synchronousProcessing = ((Flow) getFlowConstruct()).getProcessingStrategy() instanceof SynchronousProcessingStrategy;
    }
    this.poolOnPrimaryInstanceOnly =
        Boolean.valueOf(System.getProperty(MULE_TRANSPORT_FILE_SINGLEPOLLINSTANCE, "false")) || !synchronousProcessing;
    ObjectStoreManager objectStoreManager = getEndpoint().getMuleContext().getRegistry().get(OBJECT_STORE_MANAGER);
    filesBeingProcessingObjectStore = objectStoreManager.getObjectStore(getEndpoint().getName(), false, 1000, 60000, 20000);
  }

  @Override
  protected void doConnect() throws Exception {
    if (readDir != null) {
      readDirectory = FileUtils.openDirectory(readDir);
      if (!(readDirectory.canRead())) {
        throw new ConnectException(FileMessages.fileDoesNotExist(readDirectory.getAbsolutePath()), this);
      } else {
        logger.debug("Listening on endpointUri: " + readDirectory.getAbsolutePath());
      }
    }

    if (moveDir != null) {
      moveDirectory = FileUtils.openDirectory((moveDir));
      if (!(moveDirectory.canRead()) || !moveDirectory.canWrite()) {
        throw new ConnectException(FileMessages.moveToDirectoryNotWritable(), this);
      }
    }
  }

  @Override
  protected void doDisconnect() throws Exception {
    // template method
  }

  @Override
  protected void doDispose() {
    // nothing to do
  }

  @Override
  public void poll() {
    try {
      List<File> files = this.listFiles();
      if (logger.isDebugEnabled()) {
        logger.debug("Files: " + files.toString());
      }
      Comparator<File> comparator = getComparator();
      if (comparator != null) {
        Collections.sort(files, comparator);
      }
      for (File file : files) {
        if (getLifecycleState().isStopping()) {
          break;
        }
        // don't process directories
        if (file.isFile()) {
          Lock fileLock = lockFactory.createLock(file.getName());
          if (fileLock.tryLock()) {
            try {
              String fileAbsolutePath = file.getAbsolutePath();
              try {
                filesBeingProcessingObjectStore.store(fileAbsolutePath, fileAbsolutePath);

                if (logger.isDebugEnabled()) {
                  logger.debug(String.format("Flag for '%s' stored successfully.", fileAbsolutePath));
                }
              } catch (ObjectAlreadyExistsException e) {
                if (logger.isDebugEnabled()) {
                  logger.debug(String.format("Flag for '%s' being processed is on. Skipping file.", fileAbsolutePath));
                }
                continue;
              }
              if (file.exists()) {
                processFile(file);
              }
            } finally {
              fileLock.unlock();
            }
          }
        }
      }
    } catch (Exception e) {
      getEndpoint().getMuleContext().getExceptionListener().handleException(e);
    }
  }

  @Override
  protected boolean pollOnPrimaryInstanceOnly() {
    return poolOnPrimaryInstanceOnly;
  }

  public void processFile(File file) throws MuleException {
    // TODO RM*: This can be put in a Filter. Also we can add an AndFileFilter/OrFileFilter to allow users to
    // combine file filters (since we can only pass a single filter to File.listFiles, we would need to wrap
    // the current And/Or filters to extend {@link FilenameFilter}
    if (fileConnector.getCheckFileAge() && !isAgedFile(file, fileConnector.getFileAge())) {
      removeProcessingMark(file.getAbsolutePath());

      return;
    }

    // Perform some quick checks to make sure file can be processed
    if (!(file.canRead() && file.exists() && file.isFile())) {
      throw new DefaultMuleException(FileMessages.fileDoesNotExist(file.getName()));
    }

    // don't process a file that is locked by another process (probably still being written)
    if (!attemptFileLock(file)) {
      return;
    } else if (logger.isInfoEnabled()) {
      logger.info("Lock obtained on file: " + file.getAbsolutePath());
    }

    // The file may get moved/renamed here so store the original file info.
    final String originalSourceFilePath = file.getAbsolutePath();
    final String originalSourceFileName = file.getName();
    final String originalSourceDirectory = file.getParent();

    // This isn't nice but is needed as MuleMessage is required to resolve
    // destination file name
    MuleMessage fileParserMessasge =
        MuleMessage.builder().nullPayload().addInboundProperty(PROPERTY_ORIGINAL_FILENAME, originalSourceFileName)
            .addInboundProperty(PROPERTY_ORIGINAL_DIRECTORY, originalSourceDirectory).build();

    final DefaultMuleEvent event =
        new DefaultMuleEvent(create(flowConstruct, endpoint.getAddress()), fileParserMessasge, flowConstruct);

    final File sourceFile;
    if (workDir != null) {
      String workFileName = fileConnector.getFilenameParser().getFilename(event, workFileNamePattern);
      // don't use new File() directly, see MULE-1112
      File workFile = FileUtils.newFile(workDir, workFileName);

      fileConnector.move(file, workFile);
      // Now the Work File is the Source file
      sourceFile = workFile;
    } else {
      sourceFile = file;
    }

    // set up destination file
    File destinationFile = null;
    if (moveDir != null) {
      String destinationFileName = originalSourceFileName;
      if (moveToPattern != null) {
        destinationFileName = fileConnector.getFilenameParser().getFilename(event, moveToPattern);
      }
      // don't use new File() directly, see MULE-1112
      destinationFile = FileUtils.newFile(moveDir, destinationFileName);
    }

    MuleCompatibilityMessageBuilder messageBuilder = null;
    Charset encoding = endpoint.getEncoding();
    try {
      if (fileConnector.isStreaming()) {
        ReceiverFileInputStream payload =
            createReceiverFileInputStream(sourceFile, destinationFile, file1 -> removeProcessingMark(file1.getAbsolutePath()));
        messageBuilder = new MuleCompatibilityMessageBuilder(createMuleMessage(payload, encoding));
      } else {
        messageBuilder = new MuleCompatibilityMessageBuilder(createMuleMessage(sourceFile, encoding));
      }
    } catch (FileNotFoundException e) {
      // we can ignore since we did manage to acquire a lock, but just in case
      logger.error("File being read disappeared!", e);
      return;
    }

    if (workDir != null) {
      messageBuilder.addInboundProperty(PROPERTY_SOURCE_DIRECTORY, file.getParent());
      messageBuilder.addInboundProperty(PROPERTY_SOURCE_FILENAME, file.getName());
    }

    messageBuilder.addInboundProperty(PROPERTY_ORIGINAL_DIRECTORY, originalSourceDirectory);
    messageBuilder.addInboundProperty(PROPERTY_ORIGINAL_FILENAME, originalSourceFileName);

    // TODO
    messageBuilder.addOutboundProperty(PROPERTY_ORIGINAL_DIRECTORY, originalSourceDirectory);
    messageBuilder.addOutboundProperty(PROPERTY_ORIGINAL_FILENAME, originalSourceFileName);

    ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
    final MuleCompatibilityMessage finalMessage = messageBuilder.build();
    final Object originalPayload = finalMessage.getPayload();


    if (fileConnector.isStreaming()) {
      processWithStreaming(sourceFile, (ReceiverFileInputStream) originalPayload, executionTemplate, finalMessage);
    } else {
      processWithoutStreaming(originalSourceFilePath, originalSourceFileName, originalSourceDirectory, sourceFile,
                              destinationFile, executionTemplate, finalMessage);
    }
  }

  @Override
  protected void configureMuleEventBuilder(Builder builder) {
    super.configureMuleEventBuilder(builder);
    if (forceSync) {
      builder.synchronous(true);
    }
  }

  /**
   * Indicates whether or not file is older than the specified age
   *
   * @param file file to check
   * @param fileAge target file age in milliseconds
   * @return true if the file is older than the fileAge, false otherwise
   */
  protected boolean isAgedFile(File file, long fileAge) {
    final long lastMod = file.lastModified();
    final long now = System.currentTimeMillis();
    final long thisFileAge = now - lastMod;

    if (thisFileAge < fileAge) {
      if (logger.isDebugEnabled()) {
        logger.debug("The file has not aged enough yet, will return nothing for: " + file);
      }

      return false;
    }

    return true;
  }

  private void removeProcessingMark(String fileAbsolutePath) {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Removing processing flag for '%s'", fileAbsolutePath));
      }

      filesBeingProcessingObjectStore.remove(fileAbsolutePath);
    } catch (ObjectStoreException e) {
      logger.warn(String.format("Failure trying to remove file '%s' from list of files under processing", fileAbsolutePath));
    }
  }

  private void processWithoutStreaming(String originalSourceFile, final String originalSourceFileName,
                                       final String originalSourceDirectory, final File sourceFile, final File destinationFile,
                                       ExecutionTemplate<MuleEvent> executionTemplate,
                                       final MuleCompatibilityMessage finalMessage)
      throws DefaultMuleException {
    try {
      executionTemplate.execute(() -> {
        moveAndDelete(sourceFile, destinationFile, originalSourceFileName, originalSourceDirectory, finalMessage);
        return null;
      });
      deleteFileIfRequired(sourceFile, destinationFile);
    } catch (MessagingException e) {
      if (e.causedRollback()) {
        rollbackFileMoveIfRequired(originalSourceFile, sourceFile);
      } else {
        deleteFileIfRequired(sourceFile, destinationFile);
      }
    } catch (Exception e) {
      rollbackFileMoveIfRequired(originalSourceFile, sourceFile);
      getEndpoint().getMuleContext().getExceptionListener().handleException(e);
    } finally {
      removeProcessingMark(originalSourceFile);
    }
  }

  private void processWithStreaming(final File sourceFile, final ReceiverFileInputStream originalPayload,
                                    ExecutionTemplate<MuleEvent> executionTemplate, final MuleMessage finalMessage) {
    try {
      final AtomicBoolean exceptionWasThrown = new AtomicBoolean(false);
      executionTemplate.execute(() -> {
        try {
          // If we are streaming no need to move/delete now, that will be done when
          // stream is closed
          routeMessage((MuleCompatibilityMessage) new MuleCompatibilityMessageBuilder(finalMessage)
              .addOutboundProperty(PROPERTY_FILENAME, sourceFile.getName()).build());
        } catch (Exception e) {
          // ES will try to close stream but FileMessageReceiver is the one that must close it.
          exceptionWasThrown.set(true);
          originalPayload.setStreamProcessingError(true);
          throw e;
        }
        return null;
      });
      // Exception thrown but handled, consume inbound message.
      if (exceptionWasThrown.get()) {
        originalPayload.setStreamProcessingError(false);
        originalPayload.close();
      }
    } catch (MessagingException e) {
      // This code is only used by default-exception-estrategy which re-throws exception despite commit.
      if (!e.causedRollback()) {
        try {
          originalPayload.setStreamProcessingError(false);
          originalPayload.close();
        } catch (Exception ex) {
          logger.warn("Cannot close receiver file input stream", ex);
        }
      }
    } catch (Exception e) {
      getEndpoint().getMuleContext().getExceptionListener().handleException(e);
    }
  }

  /* Left for baackward compatibility */
  protected ReceiverFileInputStream createReceiverFileInputStream(File sourceFile, File destinationFile)
      throws FileNotFoundException {
    return new ReceiverFileInputStream(sourceFile, fileConnector.isAutoDelete(), destinationFile);
  }

  protected ReceiverFileInputStream createReceiverFileInputStream(File sourceFile, File destinationFile,
                                                                  InputStreamCloseListener closeListener)
      throws FileNotFoundException {
    return new ReceiverFileInputStream(sourceFile, fileConnector.isAutoDelete(), destinationFile, closeListener);
  }

  private void rollbackFileMoveIfRequired(String originalSourceFile, File sourceFile) {
    if (!sourceFile.getAbsolutePath().equals(originalSourceFile)) {
      try {
        rollbackFileMove(sourceFile, originalSourceFile);
      } catch (IOException iox) {
        logger.warn("Error rollbacking file to original location", iox);
      }
    }
  }

  private void moveAndDelete(final File sourceFile, File destinationFile, String originalSourceFileName,
                             String originalSourceDirectory, MuleCompatibilityMessage message)
      throws MuleException {
    // If we are moving the file to a read directory, move it there now and
    // hand over a reference to the
    // File in its moved location
    if (destinationFile != null) {
      // move sourceFile to new destination
      try {
        FileUtils.moveFile(sourceFile, destinationFile);
      } catch (IOException e) {
        // move didn't work - bail out (will attempt rollback)
        throw new DefaultMuleException(FileMessages.failedToMoveFile(sourceFile.getAbsolutePath(),
                                                                     destinationFile.getAbsolutePath()));
      }

      // create new Message for destinationFile
      message = (MuleCompatibilityMessage) new MuleCompatibilityMessageBuilder(createMuleMessage(destinationFile,
                                                                                                 endpoint.getEncoding()))
                                                                                                     .addInboundProperty(PROPERTY_FILENAME,
                                                                                                                         destinationFile
                                                                                                                             .getName())
                                                                                                     .addInboundProperty(PROPERTY_ORIGINAL_FILENAME,
                                                                                                                         originalSourceFileName)
                                                                                                     .addInboundProperty(PROPERTY_ORIGINAL_DIRECTORY,
                                                                                                                         originalSourceDirectory)
                                                                                                     .build();
    }

    // finally deliver the file message
    this.routeMessage(message);
  }

  private void deleteFileIfRequired(File sourceFile, File destinationFile) throws DefaultMuleException {
    // at this point msgAdapter either points to the old sourceFile
    // or the new destinationFile.
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

  /**
   * Try to acquire a lock on a file and release it immediately. Usually used as a quick check to see if another process is still
   * holding onto the file, e.g. a large file (more than 100MB) is still being written to.
   *
   * @param sourceFile file to check
   * @return <code>true</code> if the file can be locked
   */
  protected boolean attemptFileLock(final File sourceFile) throws MuleException {
    // check if the file can be processed, be sure that it's not still being
    // written
    // if the file can't be locked don't process it yet, since creating
    // a new FileInputStream() will throw an exception
    FileLock lock = null;
    FileChannel channel = null;
    boolean fileCanBeLocked = false;
    try {
      channel = new RandomAccessFile(sourceFile, "rw").getChannel();

      // Try acquiring the lock without blocking. This method returns
      // null or throws an exception if the file is already locked.
      lock = channel.tryLock();
    } catch (FileNotFoundException fnfe) {
      throw new DefaultMuleException(FileMessages.fileDoesNotExist(sourceFile.getName()));
    } catch (IOException e) {
      // Unable to create a lock. This exception should only be thrown when
      // the file is already locked. No sense in repeating the message over
      // and over.
    } finally {
      if (lock != null) {
        // if lock is null the file is locked by another process
        fileCanBeLocked = true;
        try {
          // Release the lock
          lock.release();
        } catch (IOException e) {
          // ignore
        }
      }

      if (channel != null) {
        try {
          // Close the file
          channel.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }

    return fileCanBeLocked;
  }

  /**
   * Get a list of files to be processed.
   *
   * @return an array of files to be processed.
   * @throws org.mule.api.MuleException which will wrap any other exceptions or errors.
   */
  List<File> listFiles() throws MuleException {
    try {
      List<File> files = new ArrayList<>();
      this.basicListFiles(readDirectory, files);
      return (files.isEmpty() ? NO_FILES : files);
    } catch (Exception e) {
      throw new DefaultMuleException(FileMessages.errorWhileListingFiles(), e);
    }
  }

  protected void basicListFiles(File currentDirectory, List<File> discoveredFiles) {
    File[] files = currentDirectory.listFiles();

    // the listFiles calls above may actually return null (check the JDK code).
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (file.isDirectory()) {
        if (fileConnector.isRecursive()) {
          basicListFiles(file, discoveredFiles);
        }
      } else {
        boolean addFile = true;

        if (fileFilter != null) {
          addFile = fileFilter.accept(file);
        } else if (filenameFilter != null) {
          addFile = filenameFilter.accept(currentDirectory, file.getName());
        }

        if (addFile) {
          discoveredFiles.add(file);
        }
      }
    }
  }

  /**
   * Exception tolerant roll back method
   *
   * @throws Throwable
   */
  protected void rollbackFileMove(File sourceFile, String destinationFilePath) throws IOException {
    try {
      FileUtils.moveFile(sourceFile, FileUtils.newFile(destinationFilePath));
    } catch (IOException t) {
      logger.debug("rollback of file move failed: " + t.getMessage());
      throw t;
    }
  }

  protected Comparator<File> getComparator() throws Exception {
    Object comparatorClassName = getEndpoint().getProperty(COMPARATOR_CLASS_NAME_PROPERTY);
    if (comparatorClassName != null) {
      Object reverseProperty = this.getEndpoint().getProperty(COMPARATOR_REVERSE_ORDER_PROPERTY);
      boolean reverse = false;
      if (reverseProperty != null) {
        reverse = Boolean.valueOf((String) reverseProperty);
      }

      Class<?> clazz = endpoint.getMuleContext().getExecutionClassLoader().loadClass(comparatorClassName.toString());
      Comparator<File> comparator = (Comparator<File>) clazz.newInstance();
      return reverse ? new ReverseComparator(comparator) : comparator;
    }
    return null;
  }
}

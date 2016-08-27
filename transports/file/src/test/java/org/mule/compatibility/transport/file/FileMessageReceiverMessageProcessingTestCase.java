/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.core.util.UUID;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class FileMessageReceiverMessageProcessingTestCase extends AbstractMuleTestCase {

  public static final String IMPUT_FILES_DIR = "temp";
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext mockMuleContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FileConnector mockFileConnector;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FlowConstruct mockFlowConstruct;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private InboundEndpoint mockInboundEndpoint;
  private FileMuleMessageFactory mockMessageFactory;
  @Mock
  private MuleMessage mockMessage;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessageProcessor mockMessageProcessor;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleEvent mockMuleEvent;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessagingExceptionHandler mockMessagingExceptionHandler;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessagingException mockHandledMessagingException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessagingException mockUnhandledMessagingException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessageProcessingManager mockMessageManager;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ObjectStoreManager mockObjectStoreManager;

  @Before
  public void before() {
    when(mockMuleContext.getConfiguration().getDefaultEncoding()).thenReturn("UTF-8");
  }

  /**
   *  Message processed successfully
   */
  @Test
  public void testProcessFileAndDeleteIt() throws Exception {
    configureMocks();
    configureAutoDelete();
    configureWorkingDirectory(null);
    FileMessageReceiver fileMessageReceiver = createFileMessageReceiver();
    File file = createMockFile("text.csv");
    fileMessageReceiver.processFile(file);
    assertThat(file.exists(), is(false));
  }

  /**
   *  Message processing fails but exception is handled
   */
  @Test
  public void testProcessFileThatFailsThrowHandleExceptionThenDeleteIt() throws Exception {
    configureMocks();
    configureAutoDelete();
    configureWorkingDirectory(null);
    configureListenerToThrow(mockHandledMessagingException);
    FileMessageReceiver fileMessageReceiver = createFileMessageReceiver();
    File file = createMockFile("text.csv");
    fileMessageReceiver.processFile(file);
    assertThat(file.exists(), is(false));
  }

  /**
   *  Message processing fails and exception is not handled
   */
  @Test
  public void testProcessFileThatFailsThrowsUnhandledExceptionThenDoNotDeleteIt() throws Exception {
    configureMocks();
    configureAutoDelete();
    configureWorkingDirectory(null);
    configureListenerToThrow(mockUnhandledMessagingException);
    FileMessageReceiver fileMessageReceiver = createFileMessageReceiver();
    File file = createMockFile("text.csv");
    fileMessageReceiver.processFile(file);
    assertThat(file.exists(), is(true));
  }

  /**
   *  Streaming file
   *  Message processed successfully
   */
  @Test
  public void testProcessStreamingFileTheDoNotDeleteIt() throws Exception {
    configureMocks();
    configureAutoDelete();
    configureStreaming();
    configureWorkingDirectory(null);
    FileMessageReceiver fileMessageReceiver = createFileMessageReceiver();
    File file = createMockFile("text.csv");
    fileMessageReceiver.processFile(file);
    //Using streaming, files can't be removed since the stream can still
    //in used, for instance, if we sent the payload to a vm queue
    assertThat(file.exists(), is(true));
  }

  /**
   *  Streaming file
   *  Message processing fails but exception is handled
   */
  @Test
  public void testProcessStreamingFileThatFailsThrowHandleExceptionThenDoNotDeleteIt() throws Exception {
    configureMocks();
    configureAutoDelete();
    configureStreaming();
    configureWorkingDirectory(null);
    configureListenerToThrow(mockHandledMessagingException);
    FileMessageReceiver fileMessageReceiver = createFileMessageReceiver();
    File file = createMockFile("text.csv");
    fileMessageReceiver.processFile(file);
    assertThat(file.exists(), is(false));
  }

  /**
   *  Streaming file
   *  Message processing fails and exception is not handled
   */
  @Test
  public void testProcessStreamingFileThatFailsThrowsUnhandledExceptionThenDoNotDeleteIt() throws Exception {
    configureMocks();
    configureAutoDelete();
    configureStreaming();
    configureWorkingDirectory(null);
    configureListenerToThrow(mockUnhandledMessagingException);
    FileMessageReceiver fileMessageReceiver = createFileMessageReceiver();
    File file = createMockFile("text.csv");
    fileMessageReceiver.processFile(file);
    assertThat(file.exists(), is(true));
  }

  private void configureListenerToThrow(MessagingException mockMessagingException) throws Exception {
    when(mockMessageProcessor.process(any(MuleEvent.class))).thenThrow(mockMessagingException);
  }

  private void configureStreaming() {
    when(mockFileConnector.isStreaming()).thenReturn(true);
  }

  private FileMessageReceiver createFileMessageReceiver() throws CreateException, InitialisationException {
    FileMessageReceiver fileMessageReceiver =
        new FileMessageReceiver(mockFileConnector, mockFlowConstruct, mockInboundEndpoint, IMPUT_FILES_DIR, null, null, 100) {

          @Override
          protected boolean attemptFileLock(File sourceFile) throws MuleException {
            return true;
          }

          @Override
          protected void initializeMessageFactory() throws InitialisationException {
            this.muleMessageFactory = mockMessageFactory;
          }
        };
    fileMessageReceiver.setListener(mockMessageProcessor);
    fileMessageReceiver.initialise();
    return fileMessageReceiver;
  }

  private void configureMocks() throws CreateException {
    when(mockInboundEndpoint.getConnector()).thenReturn(mockFileConnector);
    when(mockInboundEndpoint.getMuleContext()).thenReturn(mockMuleContext);
    when(mockInboundEndpoint.getFilter()).thenReturn(null);
    when(mockInboundEndpoint.getEncoding()).thenReturn(UTF_8);
    when(mockFileConnector.createMuleMessageFactory()).thenReturn(mockMessageFactory);
    mockMessageFactory = new FileMuleMessageFactory();
    when(mockMessage.getInboundProperty(MULE_ROOT_MESSAGE_ID_PROPERTY)).thenReturn(UUID.getUUID());
    when(mockHandledMessagingException.getEvent()).thenReturn(mockMuleEvent);
    when(mockUnhandledMessagingException.getEvent()).thenReturn(mockMuleEvent);
    when(mockFlowConstruct.getExceptionListener()).thenReturn(mockMessagingExceptionHandler);
    when(mockHandledMessagingException.causedRollback()).thenReturn(false);
    when(mockUnhandledMessagingException.causedRollback()).thenReturn(true);
    when(mockMessagingExceptionHandler.handleException(any(MessagingException.class), any(MuleEvent.class)))
        .thenAnswer(invocationOnMock -> {
          if (invocationOnMock.getArguments()[0] == mockHandledMessagingException) {
            return mockMuleEvent;
          } else {
            throw (Throwable) invocationOnMock.getArguments()[0];
          }
        });
    when(mockInboundEndpoint.getMuleContext().getRegistry().get(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER))
        .thenReturn(mockMessageManager);
    when(mockInboundEndpoint.getMuleContext().getRegistry().get(OBJECT_STORE_MANAGER)).thenReturn(mockObjectStoreManager);
  }

  private void configureWorkingDirectory(String workingDirectory) {
    when(mockFileConnector.getWorkDirectory()).thenReturn(workingDirectory);
  }

  private void configureAutoDelete() {
    when(mockFileConnector.isAutoDelete()).thenReturn(true);
  }

  private File createMockFile(String fileName) throws Exception {
    File file = File.createTempFile(fileName, ".txt");
    file.deleteOnExit();
    return file;
  }


}

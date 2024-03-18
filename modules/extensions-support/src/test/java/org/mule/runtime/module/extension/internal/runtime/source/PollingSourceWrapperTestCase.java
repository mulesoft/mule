/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.store.ObjectStoreSettings.DEFAULT_EXPIRATION_INTERVAL;
import static org.mule.runtime.core.internal.logger.LoggingTestUtils.verifyLogMessage;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper.WATERMARK_COMPARISON_MESSAGE;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper.WATERMARK_SAVED_MESSAGE;
import static org.mule.sdk.api.runtime.source.PollContext.PollItemStatus.ALREADY_IN_PROCESS;
import static org.mule.sdk.api.runtime.source.PollingSource.UPDATED_WATERMARK_ITEM_OS_KEY;
import static org.mule.sdk.api.runtime.source.PollingSource.WATERMARK_ITEM_OS_KEY;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.TRACE;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.logger.CustomLogger;
import org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PollingSourceWrapperTestCase {

  private static final CustomLogger logger = (CustomLogger) LoggerFactory.getLogger(PollingSourceWrapper.class);

  public static final String TEST_FLOW_NAME = "myFlow";
  public static final String EXPECTED_WATERMARK_OS = "_pollingSource_myFlow/watermark";
  public static final String EXPECTED_RECENT_IDS_OS = "_pollingSource_myFlow/recently-processed-ids";
  public static final String EXPECTED_IDS_UPDATED_WATERMARK_OS = "_pollingSource_myFlow/ids-on-updated-watermark";
  public static final String EXPECTED_INFLIGHT_IDS_OS = "_pollingSource_myFlow/inflight-ids";
  private static final String POLL_ITEM_ID = UUID.getUUID().toString();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private LockFactory lockFactoryMock;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ObjectStoreManager objectStoreManagerMock;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SchedulerService schedulerServiceMock;

  @Mock
  private ComponentLocation componentLocationMock;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SourceCallback callbackMock;

  private final PollingSource pollingSource = mock(PollingSource.class);
  private final SchedulingStrategy schedulingStrategy = mock(SchedulingStrategy.class);

  @InjectMocks
  private final PollingSourceWrapper<Object, Object> pollingSourceWrapper =
      new PollingSourceWrapper<Object, Object>(pollingSource, schedulingStrategy, 4, mock(SystemExceptionHandler.class));

  @Before
  public void setUp() throws Exception {
    when(componentLocationMock.getRootContainerName()).thenReturn(TEST_FLOW_NAME);

    when(schedulingStrategy.schedule(any(), any())).thenAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Runnable runnable = (Runnable) invocation.getArgument(1);
        runnable.run();
        return null;
      }
    });

    when(lockFactoryMock.createLock(anyString()).tryLock()).thenReturn(true);

  }

  @After
  public void restoreLoggerLevel() {
    logger.resetLevel();
  }

  @Test
  public void waterMarkingStoresGetCreatedOnStart() throws MuleException {
    pollingSourceWrapper.onStart(callbackMock);
    assertPersistentStoreIsCreated(EXPECTED_WATERMARK_OS, DEFAULT_EXPIRATION_INTERVAL);
    assertPersistentStoreIsCreated(EXPECTED_RECENT_IDS_OS, DEFAULT_EXPIRATION_INTERVAL);
    assertPersistentStoreIsCreated(EXPECTED_IDS_UPDATED_WATERMARK_OS, DEFAULT_EXPIRATION_INTERVAL);
  }

  @Test
  public void idempotencyStoreGetsCreatedOnStart() throws MuleException {
    pollingSourceWrapper.onStart(callbackMock);
    assertTransientStoreIsCreated(EXPECTED_INFLIGHT_IDS_OS, DEFAULT_EXPIRATION_INTERVAL);
  }

  @Test
  public void loggingOnAcceptedItem() throws MuleException, Exception {
    logger.resetLogs();
    logger.setLevel(DEBUG);
    stubPollItem(Collections.singletonList(null), Collections.singletonList(null));
    startSourcePollWithMockedLogger();
    verifyLogMessage(logger.getMessages(), PollingSourceWrapper.ACCEPTED_ITEM_MESSAGE, "");
  }

  @Test
  public void loggingOnRejectedItem() throws Exception {
    logger.resetLogs();
    logger.setLevel(DEBUG);
    when(lockFactoryMock.createLock(anyString()).tryLock()).thenReturn(false);
    stubPollItem(Collections.singletonList(POLL_ITEM_ID), Collections.singletonList(null));
    startSourcePollWithMockedLogger();
    verifyLogMessage(logger.getMessages(), PollingSourceWrapper.REJECTED_ITEM_MESSAGE, POLL_ITEM_ID, ALREADY_IN_PROCESS);
  }

  @Test
  public void loggingOnCreatedWatermark() throws Exception {
    logger.resetLogs();
    logger.setLevel(TRACE);
    String watermark = "5";
    stubPollItem(Collections.singletonList(POLL_ITEM_ID), Collections.singletonList(watermark));
    startSourcePollWithMockedLogger();
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, WATERMARK_ITEM_OS_KEY, watermark, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, watermark, TEST_FLOW_NAME);
  }

  @Test
  public void loggingOnUpdatedWatermark() throws Exception {
    logger.resetLogs();
    logger.setLevel(TRACE);

    List<String> ids = Arrays.asList("id1", "id2", "id3", "id4");
    List<Serializable> watermarks = Arrays.asList(1, 3, 5, 8);
    stubPollItem(ids, watermarks);
    startSourcePollWithMockedLogger();
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 1, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_COMPARISON_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 1, "itemWatermark", 3,
                     TEST_FLOW_NAME, -1);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 3, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_COMPARISON_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 3, "itemWatermark", 5,
                     TEST_FLOW_NAME, -1);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 5, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_COMPARISON_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 5, "itemWatermark", 8,
                     TEST_FLOW_NAME, -1);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 8, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, WATERMARK_ITEM_OS_KEY, 8, TEST_FLOW_NAME);
  }

  @Test
  public void loggingOnUpdatedWatermarkWithPollLimit() throws MuleException, Exception {
    logger.resetLogs();
    logger.setLevel(TRACE);
    List<String> ids = Arrays.asList("id1", "id2", "id3", "id4", "id5");
    List<Serializable> watermarks = Arrays.asList(1, 3, 5, 8, 4);
    stubPollItem(ids, watermarks);
    startSourcePollWithMockedLogger();
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 1, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_COMPARISON_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 1, "itemWatermark", 3,
                     TEST_FLOW_NAME, -1);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 3, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_COMPARISON_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 3, "itemWatermark", 5,
                     TEST_FLOW_NAME, -1);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 5, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_COMPARISON_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 5, "itemWatermark", 8,
                     TEST_FLOW_NAME, -1);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 8, TEST_FLOW_NAME);
    verifyLogMessage(logger.getMessages(), WATERMARK_COMPARISON_MESSAGE, UPDATED_WATERMARK_ITEM_OS_KEY, 8, "itemWatermark", 4,
                     TEST_FLOW_NAME, 1);
    verifyLogMessage(logger.getMessages(), WATERMARK_SAVED_MESSAGE, WATERMARK_ITEM_OS_KEY, 4, TEST_FLOW_NAME);
  }

  private void assertPersistentStoreIsCreated(String expectedName, Long expirationInterval) {
    assertStoreIsCreated(expectedName, true, expirationInterval);
  }

  private void assertTransientStoreIsCreated(String expectedName, Long expirationInterval) {
    assertStoreIsCreated(expectedName, false, expirationInterval);
  }

  private void assertStoreIsCreated(String expectedName, boolean isPersistent, Long expirationInterval) {
    ArgumentCaptor<ObjectStoreSettings> settingsCaptor = forClass(ObjectStoreSettings.class);

    verify(objectStoreManagerMock).getOrCreateObjectStore(eq(expectedName),
                                                          settingsCaptor.capture());
    ObjectStoreSettings watermarkSettings = settingsCaptor.getValue();

    assertThat(watermarkSettings.isPersistent(), is(isPersistent));
    assertThat(watermarkSettings.getExpirationInterval(), is(equalTo(expirationInterval)));
  }

  private void startSourcePollWithMockedLogger() throws Exception {
    logger.resetLogs();
    pollingSourceWrapper.onStart(callbackMock);
  }

  private void stubPollItem(List<String> pollItemIds, List<Serializable> pollItemWatermarks) {
    doAnswer(invocation -> {
      PollContext pollContext = invocation.getArgument(0, PollContext.class);
      for (int i = 0; i < pollItemIds.size(); i++) {
        String id = pollItemIds.get(i);
        Serializable watermark = pollItemWatermarks.get(i);
        pollContext
            .accept(item -> {
              if (id != null) {
                ((PollContext.PollItem) item).setId(id);
              }
              if (watermark != null) {
                ((PollContext.PollItem) item).setWatermark(watermark);
              }
              ((PollContext.PollItem) item).setResult(Result.builder().output("test").build());
            });
      } ;
      return null;
    }).when(pollingSource).poll(any());
  }

}

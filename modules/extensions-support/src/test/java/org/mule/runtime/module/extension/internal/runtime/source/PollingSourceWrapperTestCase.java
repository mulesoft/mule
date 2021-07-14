/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.store.ObjectStoreSettings.DEFAULT_EXPIRATION_INTERVAL;
import static org.mule.runtime.core.api.util.ClassUtils.setFieldValue;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;

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
import org.slf4j.Logger;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PollingSourceWrapperTestCase {

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

  @Mock
  private SourceCallback callbackMock;

  private PollingSource pollingSource = mock(PollingSource.class);
  private SchedulingStrategy schedulingStrategy = mock(SchedulingStrategy.class);
  private Logger logger = mock(Logger.class);

  @InjectMocks
  private PollingSourceWrapper<Object, Object> pollingSourceWrapper =
      new PollingSourceWrapper<Object, Object>(pollingSource, schedulingStrategy, Integer.MAX_VALUE,
                                               mock(SystemExceptionHandler.class));

  @Before
  public void setUp() throws Exception {
    when(componentLocationMock.getRootContainerName()).thenReturn(TEST_FLOW_NAME);
    setComponentLocationMock();

    when(schedulingStrategy.schedule(any(), any())).thenAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Runnable runnable = (Runnable) invocation.getArgument(1);
        runnable.run();
        return null;
      }
    });
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
    stubPollItem(Optional.empty());
    Logger origLogger = setLogger(pollingSourceWrapper, "LOGGER", logger);
    try {
      pollingSourceWrapper.onStart(callbackMock);
      verifyAcceptedItemLogMessage(logger, "");
    } finally {
      // restore original logger
      setLogger(pollingSourceWrapper, "LOGGER", origLogger);
    }
  }

  @Test
  public void loggingOnRejectedItem() throws MuleException, Exception {
    stubPollItem(Optional.of(POLL_ITEM_ID));
    Logger origLogger = setLogger(pollingSourceWrapper, "LOGGER", logger);
    try {
      pollingSourceWrapper.onStart(callbackMock);
      verifyRejectedItemLogMessage(logger, POLL_ITEM_ID, PollContext.PollItemStatus.ALREADY_IN_PROCESS);
    } finally {
      // restore original logger
      setLogger(pollingSourceWrapper, "LOGGER", origLogger);
    }
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

  private void setComponentLocationMock() throws Exception {
    setFieldValue(pollingSourceWrapper, "componentLocation", componentLocationMock, false);
  }

  private Logger setLogger(Object object, String fieldName, Logger newLogger) throws Exception {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    Logger oldLogger;
    try {
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
      try {
        oldLogger = (Logger) field.get(null);
        field.set(null, newLogger);
      } finally {
        // undo accessibility changes
        modifiersField.setInt(field, field.getModifiers());
        modifiersField.setAccessible(false);
      }
    } finally {
      // undo accessibility changes
      field.setAccessible(false);
    }
    return oldLogger;
  }

  private void stubPollItem(Optional<String> pollItemId) {
    doAnswer(new Answer() {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        PollContext pollContext = (PollContext) invocation.getArgument(0);
        pollContext
            .accept(item -> {
              if (pollItemId.isPresent()) {
                ((PollContext.PollItem) item).setId(pollItemId.get());
              }
              ((PollContext.PollItem) item).setResult(Result.builder().output("test").build());
            });
        return null;
      }
    }).when(pollingSource).poll(any());
  }

  private void verifyAcceptedItemLogMessage(Logger logger, String pollItemId) {
    verifyLogMessages(logger, String.format(PollingSourceWrapper.ACCEPTED_ITEM_MESSAGE, pollItemId));
  }

  private void verifyRejectedItemLogMessage(Logger logger, String pollItemId, PollContext.PollItemStatus status) {
    verifyLogMessages(logger, String.format(PollingSourceWrapper.REJECTED_ITEM_MESSAGE, pollItemId, status));
  }

  private void verifyLogMessages(Logger logger, String... expectedMessages) {
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(logger, atLeastOnce()).debug(argumentCaptor.capture());
    List<String> messages = argumentCaptor.getAllValues();
    assertThat(messages, hasItems(expectedMessages));
  }
}

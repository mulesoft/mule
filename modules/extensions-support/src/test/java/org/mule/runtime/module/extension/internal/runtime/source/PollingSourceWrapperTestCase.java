/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.ClassUtils.setFieldValue;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PollingSourceWrapperTestCase {

  public static final String TEST_FLOW_NAME = "myFlow";
  public static final String EXPECTED_WATERMARK_OS = "_pollingSource_myFlow/watermark";
  public static final String EXPECTED_RECENT_IDS_OS = "_pollingSource_myFlow/recently-processed-ids";
  public static final String EXPECTED_IDS_UPDATED_WATERMARK_OS = "_pollingSource_myFlow/ids-on-updated-watermark";
  public static final String EXPECTED_INFLIGHT_IDS_OS = "_pollingSource_myFlow/inflight-ids";

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

  @InjectMocks
  private PollingSourceWrapper<Object, Object> pollingSourceWrapper = new PollingSourceWrapper<Object, Object>(
                                                                                                               Mockito
                                                                                                                   .mock(PollingSource.class),
                                                                                                               Mockito
                                                                                                                   .mock(Scheduler.class));

  @Before
  public void setUp() throws Exception {
    when(componentLocationMock.getRootContainerName()).thenReturn(TEST_FLOW_NAME);
    setComponentLocationMock();
  }

  @Test
  public void waterMarkingStoresGetCreatedOnStart() throws MuleException {
    pollingSourceWrapper.onStart(callbackMock);
    assertPersistentStoreIsCreated(EXPECTED_WATERMARK_OS);
    assertPersistentStoreIsCreated(EXPECTED_RECENT_IDS_OS);
    assertPersistentStoreIsCreated(EXPECTED_IDS_UPDATED_WATERMARK_OS);
  }

  @Test
  public void idempotencyStoreGetsCreatedOnStart() throws MuleException {
    pollingSourceWrapper.onStart(callbackMock);
    assertTransientStoreIsCreated(EXPECTED_INFLIGHT_IDS_OS);
  }

  private void assertPersistentStoreIsCreated(String expectedName) {
    assertStoreIsCreated(expectedName, true);
  }

  private void assertTransientStoreIsCreated(String expectedName) {
    assertStoreIsCreated(expectedName, false);
  }

  private void assertStoreIsCreated(String expectedName, boolean isPersistent) {
    ArgumentCaptor<ObjectStoreSettings> settingsCaptor = forClass(ObjectStoreSettings.class);

    verify(objectStoreManagerMock).getOrCreateObjectStore(eq(expectedName),
                                                          settingsCaptor.capture());
    ObjectStoreSettings watermarkSettings = settingsCaptor.getValue();
    assertThat(watermarkSettings.isPersistent(), is(isPersistent));
  }

  private void setComponentLocationMock() throws Exception {
    setFieldValue(pollingSourceWrapper, "componentLocation", componentLocationMock, false);
  }

}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;

import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsynchronousRetryTemplateTestCase {

  private AsynchronousRetryTemplate template;
  @Mock(extraInterfaces = {Initialisable.class, Startable.class, Stoppable.class, Disposable.class})
  private RetryPolicyTemplate delegate;
  @Mock
  private RetryCallback callback;
  @Mock
  private Executor workManager;
  @Captor
  private ArgumentCaptor<Runnable> commandCaptor;

  @BeforeEach
  void setUp() {
    template = new AsynchronousRetryTemplate(delegate);
  }

  @Test
  void execute() throws Exception {
    final RetryContext result = template.execute(callback, workManager);

    assertThat(result, is(notNullValue()));
    verify(workManager).execute(commandCaptor.capture());
    commandCaptor.getValue().run();
    assertThat(result.isOk(), is(true));
  }

  @Test
  void isEnabled() {
    when(delegate.isEnabled()).thenReturn(true);

    assertThat(template.isEnabled(), is(true));
    verify(delegate).isEnabled();
  }

  @Test
  void createRetryInstance() {
    template.createRetryInstance();

    verify(delegate).createRetryInstance();
  }

  @Test
  void getNotifier() {
    template.getNotifier();

  }

  @Test
  void setNotifier() {}

  @Test
  void getMetaInfo() {}

  @Test
  void setMetaInfo() {}

  @Test
  void initialise() {}

  @Test
  void start() {}

  @Test
  void stop() {}

  @Test
  void dispose() {}

  @Test
  void isAsync() {}
}

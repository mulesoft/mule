/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.createMockLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.setLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.verifyLogMessage;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.verifyLogRegex;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InOrder;
import org.slf4j.Logger;

@RunWith(Parameterized.class)
public class ExtensionMessageSourceLoggingTestCase extends AbstractExtensionMessageSourceTestCase {

  private static final int TEST_TIMEOUT = 20000;
  private static final int TEST_POLL_DELAY = 1000;

  private List<String> debugMessages;
  private List<String> errorMessages;
  private Logger oldLogger;
  private ExtensionMessageSource oldSource;

  public ExtensionMessageSourceLoggingTestCase(String name, boolean primaryNodeOnly) {
    this.primaryNodeOnly = primaryNodeOnly;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"primary node only", true},
        {"all nodes", false}
    });
  }

  @Before
  public void setUp() throws Exception {
    debugMessages = new ArrayList<>();
    errorMessages = new ArrayList<>();
    oldSource = messageSource;
    messageSource = getNewExtensionMessageSourceInstance(new AsynchronousRetryTemplate(new SimpleRetryPolicyTemplate(1000, 2)));
    oldLogger = setLogger(messageSource, LOGGER_FIELD_NAME, createMockLogger(debugMessages, DEBUG));
  }

  @After
  public void restoreLogger() throws Exception {
    setLogger(messageSource, LOGGER_FIELD_NAME, oldLogger);
    after();
    messageSource = oldSource;
  }

  @Test
  public void failWithConnectionExceptionWhenRunningAndGetRetryPolicyExhausted() throws Exception {
    setLogger(messageSource, LOGGER_FIELD_NAME, createMockLogger(errorMessages, ERROR));
    start();
    ConnectionException e = new ConnectionException(ERROR_MESSAGE);
    doThrow(e).when(source).onStart(any());
    messageSource.onException(e);
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verifyLogRegex(errorMessages,
                     "Message source 'source' on flow 'appleFlow' could not be reconnected. Will be shutdown. (.*)");
      return true;
    }));

  }

  @Test
  public void sourceInitializedLogMessage() throws Exception {
    messageSource.initialise();
    if (primaryNodeOnly) {
      verifyLogMessage(debugMessages,
                       "Message source 'source' on flow 'appleFlow' running on the primary node is initializing. Note that this Message source must run on the primary node only.");
    } else {
      verifyLogMessage(debugMessages,
                       "Message source 'source' on flow 'appleFlow' is initializing. This is the primary node of the cluster.");
    }
  }

  @Test
  public void sourceStartedLogMessage() throws Exception {
    messageSource.initialise();
    messageSource.start();
    verifyLogMessage(debugMessages, "Message source 'source' on flow 'appleFlow' is starting");
  }

  @Test
  public void sourceStoppedLogMessage() throws Exception {
    messageSource.initialise();
    messageSource.start();
    messageSource.stop();
    verifyLogMessage(debugMessages, "Message source 'source' on flow 'appleFlow' is stopping");
  }

  public void start() throws Exception {
    initialise();
    if (!messageSource.getLifecycleState().isStarted()) {
      messageSource.start();
    }

    final Injector injector = muleContext.getInjector();
    InOrder inOrder = inOrder(injector, source);
    inOrder.verify(injector).inject(source);
    inOrder.verify((Initialisable) source).initialise();
    inOrder.verify(source).onStart(sourceCallback);
  }

  public void initialise() throws Exception {
    if (!messageSource.getLifecycleState().isInitialised()) {
      messageSource.initialise();
      verify(muleContext.getInjector()).inject(source);
      verify((Initialisable) source).initialise();
      verify(source, never()).onStart(sourceCallback);
    }
  }
}

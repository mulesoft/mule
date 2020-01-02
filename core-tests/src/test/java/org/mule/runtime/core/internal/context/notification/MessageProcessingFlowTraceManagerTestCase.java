/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.internal.logging.LogConfigChangeSubject;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MessageProcessingFlowTraceManagerTestCase extends AbstractMuleTestCase {

  private ServerNotificationManager notificationManager;

  private MessageProcessingFlowTraceManager manager;

  @Before
  public void before() throws InitialisationException {
    final LogConfigChangeSubject mockLogContext = mock(LogConfigChangeSubject.class, withSettings()
        .extraInterfaces(org.apache.logging.log4j.spi.LoggerContext.class));

    org.apache.logging.log4j.spi.LoggerContext loggerContext = LogManager.getContext(false);

    doAnswer(inv -> {
      ((LoggerContext) loggerContext).addPropertyChangeListener(inv.getArgument(0));
      return null;
    }).when(mockLogContext).registerLogConfigChangeListener(any());
    doAnswer(inv -> {
      ((LoggerContext) loggerContext).removePropertyChangeListener(inv.getArgument(0));
      return null;
    }).when(mockLogContext).unregisterLogConfigChangeListener(any());

    manager = new MessageProcessingFlowTraceManager() {
      @Override
      protected void withLoggerContext(Consumer<org.apache.logging.log4j.spi.LoggerContext> action) {
        action.accept((org.apache.logging.log4j.spi.LoggerContext) mockLogContext);
      }
    };

    notificationManager = mock(ServerNotificationManager.class);
    manager.setNotificationManager(notificationManager);
    manager.initialise();
  }

  @Test
  public void loggerCtxRefreshDoesntRemoveListeners() {
    ((LoggerContext) org.apache.logging.log4j.LogManager.getContext(false)).updateLoggers();
    verify(notificationManager, never()).removeListener(any());
  }

}

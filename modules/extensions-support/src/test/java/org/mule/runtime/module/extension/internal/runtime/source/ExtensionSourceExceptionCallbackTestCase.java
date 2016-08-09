/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mockito.Mockito.verify;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionSourceExceptionCallbackTestCase extends AbstractMuleTestCase {

  @Mock
  private MuleEvent event;

  @Mock
  private ResponseCompletionCallback responseCallback;

  private ExtensionSourceExceptionCallback callback;

  @Before
  public void before() {
    callback = new ExtensionSourceExceptionCallback(responseCallback, event);
  }

  @Test
  public void onException() {
    final Exception exception = new Exception();
    callback.onException(exception);
    verify(responseCallback).responseSentWithFailure(exception, event);
  }
}

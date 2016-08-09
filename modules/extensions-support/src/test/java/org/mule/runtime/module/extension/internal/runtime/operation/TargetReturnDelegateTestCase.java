/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.tck.size.SmallTest;

import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TargetReturnDelegateTestCase extends ValueReturnDelegateTestCase {

  private static final String TARGET = "myFlowVar";

  @Override
  protected ReturnDelegate createReturnDelegate() {
    return new TargetReturnDelegate(TARGET, muleContext);
  }

  @After
  public void after() {
    verify(event, never()).setMessage(any(org.mule.runtime.core.api.MuleMessage.class));
  }

  @Override
  protected MuleMessage getOutputMessage() {
    ArgumentCaptor<org.mule.runtime.core.api.MuleMessage> captor =
        ArgumentCaptor.forClass(org.mule.runtime.core.api.MuleMessage.class);
    verify(event).setFlowVariable(same(TARGET), captor.capture());
    MuleMessage message = captor.getValue();

    assertThat(message, is(notNullValue()));
    return message;
  }
}

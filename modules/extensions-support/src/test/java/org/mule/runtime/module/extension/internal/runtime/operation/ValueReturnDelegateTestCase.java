/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ValueReturnDelegateTestCase extends ValueReturnDelegateContractTestCase {

  public CursorComponentDecoratorFactory componentDecoratorFactory = NO_OP_INSTANCE;

  @Override
  protected ReturnDelegate createReturnDelegate() throws InitialisationException {
    return new ValueReturnDelegate(componentModel, componentDecoratorFactory,
                                   getCursorProviderFactory(), muleContext);
  }

  @Override
  protected Message getOutputMessage(CoreEvent result) {
    Message message = result.getMessage();

    assertThat(message, is(notNullValue()));
    return message;
  }

  @Test
  public void evaluateEvent() {
    CoreEvent event = mock(CoreEvent.class);
    assertThat(delegate.asReturnValue(event, operationContext), is(sameInstance(event)));
  }
}

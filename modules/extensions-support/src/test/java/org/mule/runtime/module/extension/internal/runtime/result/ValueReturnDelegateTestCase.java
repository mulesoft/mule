/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.result;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ValueReturnDelegateTestCase extends ValueReturnDelegateContractTestCase {

  @Override
  protected ReturnDelegate createReturnDelegate() throws InitialisationException {
    return new ValueReturnDelegate(componentModel, muleContext);
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

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.result;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class VoidReturnDelegateTestCase extends AbstractMuleTestCase {

  @Test
  public void returnsMuleEvent() throws MuleException {
    CoreEvent event = newEvent();
    ExecutionContextAdapter operationContext = mock(ExecutionContextAdapter.class);
    when(operationContext.getEvent()).thenReturn(event);

    Object returnValue = VoidReturnDelegate.INSTANCE.asReturnValue(new Object(), operationContext);
    assertThat(event, is(sameInstance(returnValue)));
  }
}

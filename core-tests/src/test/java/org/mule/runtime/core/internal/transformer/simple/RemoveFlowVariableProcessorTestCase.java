/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.mule.functional.transformer.simple.AbstractRemoveVariablePropertyProcessorTestCase;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.simple.RemoveFlowVariableProcessor;
import org.mule.tck.size.SmallTest;

import java.util.Set;

@SmallTest
public class RemoveFlowVariableProcessorTestCase extends AbstractRemoveVariablePropertyProcessorTestCase {

  public RemoveFlowVariableProcessorTestCase() {
    super(new RemoveFlowVariableProcessor());
  }

  @Override
  protected void addMockedPropeerties(CoreEvent mockEvent, Set<String> properties) {
    when(mockEvent.getVariables().keySet()).thenReturn(properties);
  }

  @Override
  protected void verifyRemoved(CoreEvent mockEvent, String key) {
    assertThat(mockEvent.getVariables().keySet(), not(contains(key)));
  }

  @Override
  protected void verifyNotRemoved(CoreEvent mockEvent, String key) {
    assertThat(mockEvent.getVariables().get(key).getValue(), not(nullValue()));
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.mule.functional.transformer.simple.AbstractRemoveVariablePropertyProcessorTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.processor.simple.RemoveFlowVariableProcessor;
import org.mule.tck.size.SmallTest;

import java.util.HashSet;

@SmallTest
public class RemoveFlowVariableProcessorTestCase extends AbstractRemoveVariablePropertyProcessorTestCase {

  public RemoveFlowVariableProcessorTestCase() {
    super(new RemoveFlowVariableProcessor());
  }

  @Override
  protected void addMockedPropeerties(MuleEvent mockEvent, HashSet properties) {
    when(mockEvent.getFlowVariableNames()).thenReturn(properties);
  }

  @Override
  protected void verifyRemoved(MuleEvent mockEvent, String key) {
    assertThat(mockEvent.getFlowVariableNames(), not(contains(key)));
  }

  @Override
  protected void verifyNotRemoved(MuleEvent mockEvent, String key) {
    assertThat(mockEvent.getFlowVariable(key), not(nullValue()));
  }
}

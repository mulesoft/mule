/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import org.mule.functional.transformer.simple.AbstractAddVariablePropertyProcessorTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.simple.AddFlowVariableProcessor;
import org.mule.tck.size.SmallTest;

@SmallTest
public class AddFlowVariableProcessorTestCase extends AbstractAddVariablePropertyProcessorTestCase {

  public AddFlowVariableProcessorTestCase() {
    super(new AddFlowVariableProcessor());
  }

  @Override
  protected void verifyAdded(CoreEvent event, String key, String value) {
    assertThat(event.getVariables().get(key).getValue(), is(value));
  }

  @Override
  protected void verifyNotAdded(CoreEvent event) {
    assertThat(event.getVariables().keySet(), empty());
  }

  @Override
  protected void verifyRemoved(CoreEvent event, String key) {
    assertThat(event.getVariables().keySet(), not(contains(key)));
  }

  @Override
  protected DataType getVariableDataType(CoreEvent event, String key) {
    return event.getVariables().get(key).getDataType();
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import org.mule.functional.transformer.simple.AbstractAddVariablePropertyProcessorTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.processor.simple.AddFlowVariableProcessor;
import org.mule.tck.size.SmallTest;

@SmallTest
public class AddFlowVariableProcessorTestCase extends AbstractAddVariablePropertyProcessorTestCase {

  public AddFlowVariableProcessorTestCase() {
    super(new AddFlowVariableProcessor());
  }

  @Override
  protected void verifyAdded(Event event, String key, String value) {
    assertThat(event.getVariable(key).getValue(), is(value));
  }

  @Override
  protected void verifyNotAdded(Event event) {
    assertThat(event.getVariableNames(), empty());
  }

  @Override
  protected void verifyRemoved(Event event, String key) {
    assertThat(event.getVariableNames(), not(contains(key)));
  }

  @Override
  protected DataType getVariableDataType(Event event, String key) {
    return event.getVariable(key).getDataType();
  }

}

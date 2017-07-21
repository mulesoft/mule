/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import org.mule.functional.transformer.simple.AbstractAddVariablePropertyProcessorTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.processor.simple.AddPropertyProcessor;
import org.mule.tck.size.SmallTest;

@SmallTest
public class AddPropertyProcessorTestCase extends AbstractAddVariablePropertyProcessorTestCase {

  public AddPropertyProcessorTestCase() {
    super(new AddPropertyProcessor());
  }

  @Override
  protected void verifyAdded(Event event, String key, String value) {
    assertThat(((InternalMessage) event.getMessage()).getOutboundProperty(key), is(value));
  }

  @Override
  protected void verifyNotAdded(Event event) {
    assertThat(((InternalMessage) event.getMessage()).getOutboundPropertyNames(), empty());
  }

  @Override
  protected void verifyRemoved(Event event, String key) {
    assertThat(((InternalMessage) event.getMessage()).getOutboundProperty(key), is(nullValue()));
  }

  @Override
  protected DataType getVariableDataType(Event event, String key) {
    return ((InternalMessage) event.getMessage()).getOutboundPropertyDataType(key);
  }

}

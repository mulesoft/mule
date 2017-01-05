/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;

import org.mule.compatibility.core.processor.simple.AddSessionVariableProcessor;
import org.mule.functional.transformer.simple.AbstractAddVariablePropertyProcessorTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.tck.size.SmallTest;

@SmallTest
@Ignore("MULE-9072 - Remove MuleSession")
public class AddSessionVariableProcessorTestCase extends AbstractAddVariablePropertyProcessorTestCase {

  public AddSessionVariableProcessorTestCase() {
    super(new AddSessionVariableProcessor());
  }

  @Override
  protected void verifyAdded(Event event, String key, String value) {
    assertThat(event.getSession().getProperty(key), is(value));
  }

  @Override
  protected void verifyNotAdded(Event event) {
    assertThat(event.getSession().getPropertyNamesAsSet(), empty());
  }

  @Override
  protected void verifyRemoved(Event event, String key) {
    assertThat(event.getSession().getProperty(key), is(nullValue()));
  }

  @Override
  protected DataType getVariableDataType(Event event, String key) {
    return event.getSession().getPropertyDataType(key);
  }
}

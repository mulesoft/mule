/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.Event;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SessionPropertiesInExceptionStrategyTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/properties/session-properties-in-exception-strategy-config.xml";
  }

  @Test
  public void sessionPropertyIsNotLost() throws Exception {
    List<String> list = new ArrayList<>();
    list.add("one");
    list.add("two");
    list.add("three");

    Event event = flowRunner("test").withPayload(list).run();

    assertThat(event.getError().isPresent(), is(false));
    assertThat(event.getMessage().getPayload().getValue(), is(notNullValue()));
    assertThat(event.getSession().getProperty("ErrorCount"), is(list.size()));
  }
}

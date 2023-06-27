/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.test.heisenberg.extension.model.HealthStatus.CANCER;
import static org.mule.test.heisenberg.extension.model.HealthStatus.DEAD;
import static org.mule.test.heisenberg.extension.model.HealthStatus.HEALTHY;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.Ricin;

import org.junit.Test;

public class SingleConfigParserTestCase extends AbstractConfigParserTestCase {

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void configWithExpressionFunctionIsSameInstanceForDifferentEvents() throws Exception {
    CoreEvent event = null;
    try {
      event = getHeisenbergEvent();
      CoreEvent anotherEvent = testEvent();
      HeisenbergExtension config = lookupHeisenberg(HEISENBERG_BYNAME, event);
      HeisenbergExtension anotherConfig = lookupHeisenberg(HEISENBERG_BYNAME, anotherEvent);
      assertThat(config, is(sameInstance(anotherConfig)));
    } finally {
      if (event != null) {
        ((BaseEventContext) event.getContext()).success();
      }
    }
  }

  @Test
  public void configWithExpressionFunctionStillDynamic() throws Exception {
    CoreEvent event = null;
    CoreEvent anotherEvent = null;
    try {
      event = getHeisenbergEvent();
      anotherEvent = CoreEvent.builder(getHeisenbergEvent()).addVariable("age", 40).build();
      HeisenbergExtension config = lookupHeisenberg(HEISENBERG_EXPRESSION, event);
      HeisenbergExtension anotherConfig = lookupHeisenberg(HEISENBERG_EXPRESSION, anotherEvent);
      assertThat(config, is(not(sameInstance(anotherConfig))));
    } finally {
      if (event != null) {
        ((BaseEventContext) event.getContext()).success();
      }
      if (anotherEvent != null) {
        ((BaseEventContext) anotherEvent.getContext()).success();
      }
    }
  }

  @Test
  public void initializedOptionalValueWithoutDefaultValue() throws Exception {
    CoreEvent event = null;
    try {
      event = getHeisenbergEvent();
      HeisenbergExtension config = lookupHeisenberg(HEISENBERG_EXPRESSION_BYREF, event);
      assertThat(config.getWeapon(), is(not(nullValue())));
      assertThat(config.getWeapon(), is(instanceOf(Ricin.class)));
    } finally {
      if (event != null) {
        ((BaseEventContext) event.getContext()).success();
      }
    }
  }

  @Test
  public void getHealthProgression() throws Exception {
    CoreEvent event = null;
    try {
      event = getHeisenbergEvent();
      HeisenbergExtension config = lookupHeisenberg(HEISENBERG_BYNAME, event);
      assertThat(config.getHealthProgression(), is(not(nullValue())));
      assertThat(config.getHealthProgression().size(), is(3));
      assertThat(config.getHealthProgression(), contains(HEALTHY, CANCER, DEAD));
    } finally {
      if (event != null) {
        ((BaseEventContext) event.getContext()).success();
      }
    }
  }
}

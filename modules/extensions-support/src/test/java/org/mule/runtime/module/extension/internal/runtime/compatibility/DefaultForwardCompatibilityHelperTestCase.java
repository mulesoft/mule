/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.compatibility;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROFILING_SERVICE_PROPERTY;

import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.module.extension.api.runtime.compatibility.DefaultForwardCompatibilityHelper;
import org.mule.sdk.compatibility.api.utils.ForwardCompatibilityHelper;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class DefaultForwardCompatibilityHelperTestCase extends AbstractMuleContextTestCase {

  @Rule
  public SystemProperty systemProperty = new SystemProperty(ENABLE_PROFILING_SERVICE_PROPERTY, "true");

  @Test
  public void isDefaultImplementation() {
    assertThat(ForwardCompatibilityHelper.getInstance(), instanceOf(DefaultForwardCompatibilityHelper.class));
  }

  @Test
  public void getDistributedTraceContextManager() {
    ForwardCompatibilityHelper helper = ForwardCompatibilityHelper.getInstance();
    helper.getDistributedTraceContextManager(mock(CorrelationInfo.class));
  }
}

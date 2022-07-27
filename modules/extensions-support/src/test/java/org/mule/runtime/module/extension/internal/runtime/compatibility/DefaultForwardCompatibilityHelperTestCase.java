/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.compatibility;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.runtime.module.extension.api.runtime.compatibility.DefaultForwardCompatibilityHelper;
import org.mule.sdk.api.utils.ForwardCompatibilityHelper;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DefaultForwardCompatibilityHelperTestCase extends AbstractMuleContextTestCase {

  @Test
  public void isDefaultImplementation() {
    assertThat(ForwardCompatibilityHelper.getInstance(), instanceOf(DefaultForwardCompatibilityHelper.class));
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation;

import static org.mule.runtime.module.artifact.activation.internal.ExecutionContext.isMuleFramework;
import static org.mule.test.allure.AllureConstants.ExecutionContextFeature.EXECUTION_CONTEXT;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(EXECUTION_CONTEXT)
public class ExecutionContextTestCase extends AbstractMuleTestCase {

  @Test
  public void notInMuleFrameworkContext() {
    assertThat(isMuleFramework(), is(false));
  }

}

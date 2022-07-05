/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation;

import static org.mule.runtime.module.artifact.activation.internal.ExecutionEnvironment.isMuleFramework;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class ExecutionEnvironmentTestCase extends AbstractMuleTestCase {

  @Test
  public void notInMuleFrameworkContext() {
    assertThat(isMuleFramework(), is(false));
  }

}

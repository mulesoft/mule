/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

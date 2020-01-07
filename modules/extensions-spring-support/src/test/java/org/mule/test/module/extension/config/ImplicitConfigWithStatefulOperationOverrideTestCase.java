/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class ImplicitConfigWithStatefulOperationOverrideTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "implicit-stateful-override-config.xml";
  }

  @Test
  public void statefulOverride() throws Exception {
    String response = flowRunner("statefulOverride").run().getMessage().getPayload().getValue().toString();
    assertThat(response, equalTo("implicit-config-implicit 42"));
  }
}

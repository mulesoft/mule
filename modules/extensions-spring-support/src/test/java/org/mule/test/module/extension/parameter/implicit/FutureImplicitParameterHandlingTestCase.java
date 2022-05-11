/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.implicit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.secretSdkFutureFeature;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class FutureImplicitParameterHandlingTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "future-implicit-config.xml";
  }

  @Test
  public void sdkFutureImplicitHandling() throws Exception {
    flowRunner("futureSdkImplicitHandling");
    assertThat(secretSdkFutureFeature, is(nullValue()));
  }
}

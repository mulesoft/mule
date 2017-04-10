/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.test.vegan.extension.VeganPolicy;

import org.junit.Test;

public class NullSafeOnImplicitElementTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vegan-null-safe-on-implicit-elements-config.xml";
  }

  @Test
  public void nullSafeOnImplicitConnectionProvider() throws Exception {
    VeganPolicy policy = (VeganPolicy) flowRunner("grape-operation").run().getMessage().getPayload().getValue();
    assertThat(policy, is(notNullValue()));
    assertThat(policy.getMeetAllowed(), is(false));
    assertThat(policy.getMaxCalories(), is(500));
  }
}

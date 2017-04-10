/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.test.implicit.config.extension.extension.Counter;
import org.mule.test.implicit.config.extension.extension.ImplicitConfigExtension;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class ImplicitConfigTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "implicit-config.xml";
  }

  @Test
  public void getImplicitConfig() throws Exception {
    final Integer defaultValue = 42;
    ImplicitConfigExtension config = (ImplicitConfigExtension) flowRunner("implicitConfig").withPayload("")
        .withVariable("optionalWithDefault", defaultValue).withVariable("number", 5).run().getMessage().getPayload().getValue();


    assertThat(config, is(notNullValue()));
    assertThat(config.getMuleContext(), is(sameInstance(muleContext)));
    assertThat(config.getInitialise(), is(1));
    assertThat(config.getStart(), is(1));
    assertThat(config.getOptionalNoDefault(), is(nullValue()));
    assertThat(config.getOptionalWithDefault(), is(defaultValue));
  }

  @Test
  public void getImplicitConnection() throws Exception {
    Object connection = flowRunner("implicitConnection").withVariable("number", 5).run().getMessage().getPayload().getValue();
    assertThat(connection, is(instanceOf(Counter.class)));
  }
}

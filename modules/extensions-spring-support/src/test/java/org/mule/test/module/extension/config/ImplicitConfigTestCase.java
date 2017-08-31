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

  private static final int PARAMETER_DEFAULT_VALUE = 5;
  private static final int DEFAULT_VALUE_FROM_EXPRESSION = 42;

  @Override
  protected String getConfigFile() {
    return "implicit-config.xml";
  }

  @Test
  public void getImplicitConfig() throws Exception {
    ImplicitConfigExtension config = (ImplicitConfigExtension) flowRunner("implicitConfig").withPayload("")
        .withVariable("optionalWithDefault", DEFAULT_VALUE_FROM_EXPRESSION).withVariable("number", PARAMETER_DEFAULT_VALUE).run()
        .getMessage().getPayload().getValue();

    assertThat(config, is(notNullValue()));
    assertThat(config.getMuleContext(), is(sameInstance(muleContext)));
    assertThat(config.getInitialise(), is(1));
    assertThat(config.getStart(), is(1));
    assertThat(config.getOptionalNoDefault(), is(nullValue()));
    assertThat(config.getOptionalWithDefault(), is(PARAMETER_DEFAULT_VALUE));
  }

  @Test
  public void getImplicitConnection() throws Exception {
    Object connection = flowRunner("implicitConnection").withVariable("number", PARAMETER_DEFAULT_VALUE).run().getMessage()
        .getPayload().getValue();
    assertThat(connection, is(instanceOf(Counter.class)));
  }

  @Test
  public void getImplicitConfigNullSafeParameter() throws Exception {
    ImplicitConfigExtension config = (ImplicitConfigExtension) flowRunner("implicitConfig").withPayload("")
        .withVariable("optionalWithDefault", DEFAULT_VALUE_FROM_EXPRESSION).withVariable("number", PARAMETER_DEFAULT_VALUE).run()
        .getMessage().getPayload().getValue();

    assertThat(config, is(notNullValue()));
    assertThat(config.getMuleContext(), is(sameInstance(muleContext)));

    assertThat(config.getNullSafeGroup(), is(notNullValue()));
    assertThat(config.getNullSafeGroup().getNullSafePojo(), is(notNullValue()));
    assertThat(config.getNullSafeGroup().getNullSafePojo().getNullSafeInteger(), is(PARAMETER_DEFAULT_VALUE));

    assertThat(config.getNullSafeGroupShowInDsl(), is(notNullValue()));
    assertThat(config.getNullSafeGroupShowInDsl().getNullSafePojoShowInDsl(), is(notNullValue()));
    assertThat(config.getNullSafeGroupShowInDsl().getNullSafePojoShowInDsl().getNullSafeInteger(), is(PARAMETER_DEFAULT_VALUE));
  }
}

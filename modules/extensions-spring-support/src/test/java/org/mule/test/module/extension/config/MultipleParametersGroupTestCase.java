/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.message.Message;
import org.mule.test.heisenberg.extension.model.BarberPreferences;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

public class MultipleParametersGroupTestCase extends AbstractExtensionFunctionalTestCase {

  public static final String HEISENBERG = "heisenberg";
  private static final String EMPTY_STRING = "";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"heisenberg-multiple-parameters-group-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void getInlineGroupDefinition() throws Exception {
    Message message = flowRunner("getBarberPreferences").withPayload(EMPTY_STRING).run().getMessage();

    assertThat(message.getPayload().getValue(), is(notNullValue()));
    assertThat(message.getPayload().getDataType().getMediaType().matches(APPLICATION_JAVA), is(true));

    BarberPreferences preferences = (BarberPreferences) message.getPayload().getValue();
    assertThat(preferences.getBeardTrimming(), is(BarberPreferences.BEARD_KIND.MUSTACHE));
    assertThat(preferences.isFullyBald(), is(false));

    message = flowRunner("getSecondBarberPreferences").withPayload(EMPTY_STRING).run().getMessage();

    assertThat(message.getPayload().getValue(), is(notNullValue()));
    assertThat(message.getPayload().getDataType().getMediaType().matches(APPLICATION_JAVA), is(true));

    preferences = (BarberPreferences) message.getPayload().getValue();
    assertThat(preferences.getBeardTrimming(), is(BarberPreferences.BEARD_KIND.GOATIE));
    assertThat(preferences.isFullyBald(), is(true));


  }
}

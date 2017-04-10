/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static java.nio.charset.Charset.availableCharsets;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetCage;

import org.junit.Before;
import org.junit.Test;

public class PetStoreDefaultEncodingTestCase extends AbstractExtensionFunctionalTestCase {

  private String defaultEncoding;
  private String customEncoding;

  @Override
  protected String getConfigFile() {
    return "petstore-default-mule-encoding.xml";
  }

  @Before
  public void setUp() {
    defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
    assertThat(defaultEncoding, is(notNullValue()));

    customEncoding =
        availableCharsets().keySet().stream().filter(encoding -> !encoding.equals(defaultEncoding)).findFirst().orElse(null);
    assertThat(customEncoding, is(notNullValue()));
    assertThat(customEncoding, is(not(defaultEncoding)));
  }

  @Test
  public void configEncoding() throws Exception {
    PetCage cage = (PetCage) flowRunner("fieldEncoding").run().getMessage().getPayload().getValue();
    assertDefaultEncoding(cage.getEncoding());
  }

  @Test
  public void topLevelEncoding() throws Exception {
    assertDefaultEncoding((String) flowRunner("topLevelEncoding").run().getMessage().getPayload().getValue());
  }

  @Test
  public void argumentEncoding() throws Exception {
    assertDefaultEncoding((String) flowRunner("argumentEncoding").run().getMessage().getPayload().getValue());
  }

  @Test
  public void overrideEncoding() throws Exception {
    String flowResult = (String) flowRunner("overridedArgumentEncoding").withVariable("customEncoding", customEncoding).run()
        .getMessage().getPayload().getValue();
    assertThat(flowResult, is(customEncoding));
  }

  private void assertDefaultEncoding(String encoding) {
    assertThat(encoding, notNullValue());
    assertThat(encoding, is(defaultEncoding));
  }

}

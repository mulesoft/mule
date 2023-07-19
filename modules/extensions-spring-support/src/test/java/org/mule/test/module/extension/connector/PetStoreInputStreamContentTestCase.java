/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class PetStoreInputStreamContentTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-input-stream-content-parameter.xml";
  }

  @Test
  public void parseInputStreamContentParameter() throws Exception {
    String fish = (String) flowRunner("getFish")
        .withVariable("herring", new ByteArrayInputStream("herring".getBytes()))
        .run().getMessage().getPayload().getValue();

    assertThat(fish, is("goldfish herring"));
  }
}

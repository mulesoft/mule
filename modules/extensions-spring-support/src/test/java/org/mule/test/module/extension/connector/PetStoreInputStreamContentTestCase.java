/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

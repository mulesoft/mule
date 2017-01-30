/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

import org.junit.Test;

public class PetStoreInputeStreamContentTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-input-stream-content-parameter.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Test
  public void parseInputStreamContentParameter() throws Exception {
    String fish = (String) flowRunner("getFish").run().getMessage().getPayload().getValue();
    assertThat(fish, is("goldfish herring"));
  }
}

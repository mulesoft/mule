/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetCage;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class PetStoreComplexParameterTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-complex-parameter.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Test
  public void configWithConfigReferences() throws Exception {
    PetCage cage = flowRunner("getCageWithReferences").run().getMessage().getPayload();
    assertBirds(cage.getBirds());
    assertAmmenities(cage.getAmmenities());
  }

  @Test
  public void configWithConfigMELReferences() throws Exception {
    PetCage cage = flowRunner("getCageWithMELReferences").run().getMessage().getPayload();
    assertBirds(cage.getBirds());
    assertAmmenities(cage.getAmmenities());
  }

  @Test
  public void configWithConfigChildElements() throws Exception {
    PetCage cage = flowRunner("getCageWithChildElements").run().getMessage().getPayload();
    assertBirds(cage.getBirds());
    assertAmmenities(cage.getAmmenities());
  }

  private void assertBirds(Map<String, Integer> birds) {
    assertNotNull(birds);
    assertThat(birds.get("mockingjay"), equalTo(15));
    assertThat(birds.get("mockingbird"), equalTo(10));
  }

  private void assertAmmenities(List<String> ammenities) {
    assertNotNull(ammenities);
    assertThat(ammenities.get(0), equalTo("spinning wheel"));
    assertThat(ammenities.get(1), equalTo("food can"));
  }
}

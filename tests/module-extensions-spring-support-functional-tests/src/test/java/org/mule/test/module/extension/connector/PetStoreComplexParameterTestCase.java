/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetCage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class PetStoreComplexParameterTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-complex-parameter.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    final Map<String, Object> objects = new HashMap<>();

    objects.put("birds", ImmutableMap.<String, Integer>builder()
        .put("mockingjay", 15)
        .put("mockingbird", 10)
        .build());
    objects.put("ammenities", ImmutableList.<String>builder()
        .add("spinning wheel")
        .add("food can")
        .build());

    return objects;
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void configWithConfigReferences() throws Exception {
    PetCage cage = (PetCage) flowRunner("getCageWithReferences").run().getMessage().getPayload().getValue();
    assertBirds(cage.getBirds());
    assertAmmenities(cage.getAmmenities());
  }

  @Test
  public void configWithConfigMELReferences() throws Exception {
    PetCage cage = (PetCage) flowRunner("getCageWithMELReferences").run().getMessage().getPayload().getValue();
    assertBirds(cage.getBirds());
    assertAmmenities(cage.getAmmenities());
  }

  @Test
  public void configWithConfigChildElements() throws Exception {
    PetCage cage = (PetCage) flowRunner("getCageWithChildElements").run().getMessage().getPayload().getValue();
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

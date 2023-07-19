/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.Pet;

import java.util.List;

import org.junit.Test;

public class PetStoreEnumTransformationTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-enum-list-transformer.xml";
  }

  @Test
  public void enumListTransformationTest() throws Exception {
    List<Pet> pets = (List<Pet>) flowRunner("getForbiddenPetWithExpression").run().getMessage().getPayload().getValue();
    assertThat(pets.size(), is(2));
    assertThat(pets.get(0), is(Pet.DOG));
    assertThat(pets.get(1), is(Pet.CAT));

  }
}

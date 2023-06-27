/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

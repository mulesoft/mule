/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.ExclusivePetBreeder;

import org.junit.Test;

public class PetStoreExclusiveParameterRequiredWithAliasTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-exclusive-parameters-required-with-alias.xml";
  }

  @Test
  public void getBreederOperation() throws Exception {
    ExclusivePetBreeder petBreeder = (ExclusivePetBreeder) runFlow("getBreederOperation").getMessage().getPayload().getValue();
    assertThat(petBreeder.getunaliasedNammals(), is("Primate"));
    assertThat(petBreeder.getBirds(), is(nullValue()));
  }
}

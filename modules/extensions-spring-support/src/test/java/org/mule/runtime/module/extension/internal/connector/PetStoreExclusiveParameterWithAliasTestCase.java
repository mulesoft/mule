/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.InvalidExtensionConfigTestCase;
import org.mule.test.petstore.extension.ExclusivePetBreeder;
import org.mule.test.petstore.extension.PetStoreConnector;

import org.junit.Test;

public class PetStoreExclusiveParameterWithAliasTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Override
  protected String getConfigFile() {
    return "petstore-exclusive-parameters-with-alias.xml";
  }

  @Test
  public void getBreederOperation() throws Exception {
    ExclusivePetBreeder petBreeder = (ExclusivePetBreeder) runFlow("getBreederOperation").getMessage().getPayload().getValue();
    assertThat(petBreeder.getunaliasedNammals(), is("Primate"));
    assertThat(petBreeder.getBirds(), is(nullValue()));
  }
}

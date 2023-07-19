/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.List;

public class PetStoreNonStringStringTypeParameterTestCase extends AbstractExtensionFunctionalTestCase {

  private static Character CHARACTER = 'c';
  private static char CHAR = 'c';
  private static String CLAZZ_NAME = List.class.getName();

  @Override
  protected String getConfigFile() {
    return "petstore-non-string-string-type-parameter.xml";
  }

  @Test
  public void testOperationWithCharacterParameter() throws Exception {
    Character character =
        (Character) flowRunner("spellObject").withVariable("character", CHARACTER).run().getMessage().getPayload().getValue();
    assertThat(character, is(CHARACTER));
  }

  @Test
  public void testOperationWithCharParameter() throws Exception {
    char character =
        (char) flowRunner("spellBuiltIn").withVariable("char", CHAR).run().getMessage().getPayload().getValue();
    assertThat(character, is(CHAR));
  }

  @Test
  public void testOperationWithClassParameter() throws Exception {
    Class clazz =
        (Class) flowRunner("spellClass").withVariable("class", CLAZZ_NAME).run().getMessage().getPayload().getValue();
    assertThat(clazz.getName(), is(CLAZZ_NAME));
  }

}

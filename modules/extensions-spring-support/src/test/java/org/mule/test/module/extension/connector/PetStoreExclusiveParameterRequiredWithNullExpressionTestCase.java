/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;


import org.junit.Test;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.ExclusivePetBreeder;

public class PetStoreExclusiveParameterRequiredWithNullExpressionTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-exclusive-parameters-required-with-null-expression.xml";
  }

  @Test
  public void getBreederOperation() throws Exception {
    flowRunner("getBreederOperation").withVariable("mammals", "hola");
  }
}

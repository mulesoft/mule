/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.junit.Assert.fail;

import org.junit.Test;

public class OverriddenSubstitutionGroupTestCase extends AbstractExtensionFunctionalTestCase {


  @Override
  protected String getConfigFile() {
    return "substitution-group-schema-validation.xml";
  }

  @Test
  public void testSchemaIsCorrectlyGenerated() throws Exception {
    //If not exception is because the schema generation is ok
    try {
      flowRunner("doNothing").run();
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

}


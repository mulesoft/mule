/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
    // If not exception is because the schema generation is ok
    try {
      flowRunner("doNothing").run();
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

}


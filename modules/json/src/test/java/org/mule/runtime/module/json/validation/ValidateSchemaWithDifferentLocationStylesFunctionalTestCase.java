/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.validation;

import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getGoodFstab;

import org.junit.Test;

public class ValidateSchemaWithDifferentLocationStylesFunctionalTestCase extends AbstractValidateSchemaFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "validate-schema-location-config.xml";
  }

  @Test
  public void withRelativePath() throws Exception {
    flowRunner("withRelativePath").withPayload(getGoodFstab()).run();
  }

  @Test
  public void withRelativePathStartingWithSlash() throws Exception {
    flowRunner("withRelativePathStartingWithSlash").withPayload(getGoodFstab()).run();
  }

  @Test
  public void withUri() throws Exception {
    flowRunner("withUri").withPayload(getGoodFstab()).run();
  }
}

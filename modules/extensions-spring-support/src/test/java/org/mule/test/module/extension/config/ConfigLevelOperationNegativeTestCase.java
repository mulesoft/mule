/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import org.mule.test.module.extension.InvalidExtensionConfigTestCase;

public class ConfigLevelOperationNegativeTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "vegan-invalid-config-for-operations.xml";
  }
}

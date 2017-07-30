/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional.compatibility;

import org.mule.test.functional.ModuleXsdCustomTypeTestCase;

/**
 * TODO MULE-13214: compatibility test, could be removed once MULE-13214 is done
 */
public class CompatibilityModuleXsdCustomTypeTestCase extends ModuleXsdCustomTypeTestCase {

  @Override
  protected String getModulePath() {
    return "compatibility/" + super.getModulePath();
  }

  @Override
  protected String getConfigFile() {
    return "compatibility/" + super.getConfigFile();
  }
}

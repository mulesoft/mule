/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED;

import org.junit.BeforeClass;

public class CachedExtensionsClientFunctionalTestCase extends AbstractExtensionsClientFunctionalTestCase {

  @BeforeClass
  public static void setUpStrategy() {
    System.setProperty(MULE_EXTENSIONS_CLIENT_CACHE_IS_DISABLED, "false");
  }

}


/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client;

import static java.lang.System.setProperty;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_EXTENSION_CLIENT_CACHE_DISABLED;

import org.junit.BeforeClass;

public class NonCachedBlockingExtensionsClientTestCase extends BlockingExtensionsClientTestCase {

  @BeforeClass
  public static void setDisableExtensionsClientCacheSystemProperty() throws Exception{
    setProperty(MULE_EXTENSION_CLIENT_CACHE_DISABLED, "true");
  }

}

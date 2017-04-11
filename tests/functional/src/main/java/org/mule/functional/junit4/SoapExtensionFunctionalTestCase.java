/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader;

public abstract class SoapExtensionFunctionalTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return new SoapExtensionModelLoader();
  }

  @Override
  protected boolean mockHttpService() {
    return true;
  }

  @Override
  protected boolean mockExprExecutorService() {
    return false;
  }
}

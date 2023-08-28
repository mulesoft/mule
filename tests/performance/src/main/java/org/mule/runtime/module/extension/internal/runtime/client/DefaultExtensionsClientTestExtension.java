/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Operations;

@Extension(name = DefaultExtensionsClientTestExtension.DEFAULT_EXTENSIONS_CLIENT_TEST_EXTENSION_NAME)
@Operations(DefaultExtensionsClientTestOperations.class)
public class DefaultExtensionsClientTestExtension {

  public static final String DEFAULT_EXTENSIONS_CLIENT_TEST_EXTENSION_NAME = "Default Extensions Client Test";
}

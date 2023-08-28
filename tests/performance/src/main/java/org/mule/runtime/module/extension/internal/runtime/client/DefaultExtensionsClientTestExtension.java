/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Operations;

@Extension(name = DefaultExtensionsClientTestExtension.DEFAULT_EXTENSIONS_CLIENT_TEST_EXTENSION_NAME)
@Operations(DefaultExtensionsClientTestOperations.class)
public class DefaultExtensionsClientTestExtension {

  public static final String DEFAULT_EXTENSIONS_CLIENT_TEST_EXTENSION_NAME = "Default Extensions Client Test";
}

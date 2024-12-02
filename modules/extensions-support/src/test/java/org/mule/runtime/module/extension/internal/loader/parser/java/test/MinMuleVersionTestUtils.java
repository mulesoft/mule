/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.test;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.util.Collections.emptySet;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;

public class MinMuleVersionTestUtils {

  public static ExtensionLoadingContext ctxResolvingMinMuleVersion() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    return new DefaultExtensionLoadingContext(builder(contextClassLoader, getDefault(emptySet()))
        .setResolveMinMuleVersion(true).build());
  }
}

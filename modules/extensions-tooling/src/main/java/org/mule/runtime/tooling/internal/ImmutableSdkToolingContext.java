/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.internal;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tooling.internal.command.SdkToolingContext;

import java.util.Map;

public class ImmutableSdkToolingContext implements SdkToolingContext {

  private final ExtensionModel extensionModel;
  private final Map<String, Object> parameters;
  private final MuleContext muleContext;
  private final ClassLoader classLoader;

  public ImmutableSdkToolingContext(ExtensionModel extensionModel,
                                    Map<String, Object> parameters,
                                    MuleContext muleContext,
                                    ClassLoader classLoader) {
    this.extensionModel = extensionModel;
    this.parameters = parameters;
    this.muleContext = muleContext;
    this.classLoader = classLoader;
  }

  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.policy.api.extension;

import static org.mule.functional.policy.api.extension.TestPolicyExtensionModelProvider.getExtensionModel;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider;

/**
 * A {@link RuntimeExtensionModelProvider} for test Policies components.
 *
 * @since 4.4
 */
@NoInstantiate
public class TestPolicyRuntimeExtensionModelProvider implements RuntimeExtensionModelProvider {

  @Override
  public ExtensionModel createExtensionModel() {
    return getExtensionModel();
  }

}

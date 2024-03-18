/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import static org.mule.runtime.jpms.api.JpmsUtils.validateNoBootModuleLayerTweaking;

/**
 * A {@link BootstrapConfigurer} that takes care of validating the boot module layer has not been tampered with.
 *
 * @since 4.6
 */
public class BootModuleLayerValidationBootstrapConfigurer implements BootstrapConfigurer {

  public boolean configure() throws BootstrapConfigurationException {
    try {
      validateNoBootModuleLayerTweaking();
      return true;
    } catch (Exception e) {
      throw new BootstrapConfigurationException(1, e);
    }
  }
}

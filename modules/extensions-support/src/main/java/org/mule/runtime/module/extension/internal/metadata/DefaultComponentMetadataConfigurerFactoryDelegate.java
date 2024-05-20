/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurer;
import org.mule.runtime.extension.internal.metadata.ComponentMetadataConfigurerFactoryDelegate;
import org.mule.runtime.module.extension.api.metadata.DefaultComponentMetadataConfigurer;

public class DefaultComponentMetadataConfigurerFactoryDelegate implements ComponentMetadataConfigurerFactoryDelegate {

  @Override
  public ComponentMetadataConfigurer create() {
    return new DefaultComponentMetadataConfigurer();
  }
}

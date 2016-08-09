/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.runtime.container.api.MuleCoreExtension;

public class MuleCoreExtensionProcessConfigurer implements ProcessBuilderConfigurer {

  private final Class<? extends MuleCoreExtension>[] coreExtensionClasses;

  public MuleCoreExtensionProcessConfigurer(Class<? extends MuleCoreExtension>... coreExtensionClasses) {
    if (coreExtensionClasses == null || coreExtensionClasses.length == 0) {
      throw new IllegalArgumentException("Could not create core extension configurer without core extensions");
    }
    this.coreExtensionClasses = coreExtensionClasses;
  }

  @Override
  public void configure(String instanceId, ConfigurableProcessBuilder configurableProcessBuilder) {
    StringBuilder propertyValue = new StringBuilder();
    for (Class<? extends MuleCoreExtension> coreExtensionClass : coreExtensionClasses) {
      propertyValue.append(coreExtensionClass.getName() + ",");
    }
    configurableProcessBuilder.addConfigurationAttribute(MuleContextProcessBuilder.MULE_CORE_EXTENSIONS_PROPERTY,
                                                         propertyValue.substring(0, propertyValue.length() - 1));
  }
}

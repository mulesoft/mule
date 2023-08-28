/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

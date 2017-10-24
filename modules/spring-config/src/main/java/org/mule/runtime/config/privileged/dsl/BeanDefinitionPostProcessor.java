/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.privileged.dsl;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.Set;

/**
 * @since 4.0
 */
public interface BeanDefinitionPostProcessor {

  default void adaptBeanDefinition(ComponentConfiguration parentComponentConfiguration, Class beanClass,
                                   PostProcessorIocHelper iocHelper) {
    // Nothing to do
  }

  void postProcess(ComponentConfiguration componentConfiguration, PostProcessorIocHelper iocHelper);

  default Set<ComponentIdentifier> getGenericPropertiesCustomProcessingIdentifiers() {
    return emptySet();
  }
}

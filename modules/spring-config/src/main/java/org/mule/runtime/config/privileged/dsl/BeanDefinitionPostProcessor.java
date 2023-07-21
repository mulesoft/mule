/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.privileged.dsl;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.Set;

/**
 * @since 4.0
 */
public interface BeanDefinitionPostProcessor {

  /**
   * @deprecated This is no longer called since 4.3
   */
  @Deprecated
  default void adaptBeanDefinition(ComponentConfiguration parentComponentConfiguration, Class beanClass,
                                   PostProcessorIocHelper iocHelper) {
    // Nothing to do
  }

  /**
   * @deprecated This is no longer called since 4.3
   */
  @Deprecated
  void postProcess(ComponentConfiguration componentConfiguration, PostProcessorIocHelper iocHelper);

  default Set<ComponentIdentifier> getGenericPropertiesCustomProcessingIdentifiers() {
    return emptySet();
  }
}

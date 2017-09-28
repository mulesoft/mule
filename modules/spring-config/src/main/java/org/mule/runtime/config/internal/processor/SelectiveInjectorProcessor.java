/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.processor;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;

/**
 * Base class for specializations of {@link MuleInjectorProcessor} which should only perform injection if a certain condition is
 * met.
 *
 * @since 3.7.0
 */
abstract class SelectiveInjectorProcessor extends MuleInjectorProcessor {

  /**
   * Only performs the injetion if {@link #shouldInject(PropertyValues, PropertyDescriptor[], Object, String)} returns
   * {@code true} {@inheritDoc}
   */
  @Override
  public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName)
      throws BeansException {
    if (shouldInject(pvs, pds, bean, beanName)) {
      return super.postProcessPropertyValues(pvs, pds, bean, beanName);
    }

    return pvs;
  }

  /**
   * @return whether the injection should be performed or not
   */
  protected abstract boolean shouldInject(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName);
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import java.util.List;

/**
 * Bean dependency resolver interface.
 * <p/>
 * Implementation of this interface must resolve the dependencies between beans in the spring context.
 * 
 * @since 4.0
 */
public interface BeanDependencyResolver {

  /**
   * @param beanName the name of the bean to resolve dependencies
   * @return a order collection of bean objects.
   */
  List<Object> resolveBeanDependencies(String beanName);

}

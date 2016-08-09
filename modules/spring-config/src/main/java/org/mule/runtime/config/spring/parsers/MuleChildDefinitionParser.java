/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers;

import org.mule.runtime.config.spring.parsers.assembly.configuration.PropertyConfiguration;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Extra functionality exposed by child parsers
 */
public interface MuleChildDefinitionParser extends MuleDefinitionParser {

  public void forceParent(BeanDefinition parent);

  public PropertyConfiguration getTargetPropertyConfiguration();

}

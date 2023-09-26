/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.privileged.dsl.spring;

import java.util.function.Supplier;

import org.springframework.beans.factory.SmartFactoryBean;

/**
 * This interface is used to implement the getters and setters of the fields added with Byte Buddy. It also extends from
 * {@link SmartFactoryBean}.
 *
 * @since 4.6.0
 */
public interface SmartFactoryBeanInterceptor {

  Boolean getIsSingleton();

  void setIsSingleton(Boolean isSingleton);

  Class getObjectTypeClass();

  boolean getIsPrototype();

  void setIsPrototype(Boolean isPrototype);

  Supplier getIsEagerInit();

  void setIsEagerInit(Supplier isEagerInit);
}

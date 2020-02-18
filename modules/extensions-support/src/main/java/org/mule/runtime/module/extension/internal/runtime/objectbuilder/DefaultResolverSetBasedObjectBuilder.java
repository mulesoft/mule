/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilderUtils.createInstance;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Default implementation of {@link ResolverSetBasedObjectBuilder} which uses reflection
 * and a default constructor to instantiate the generated objects
 *
 * @param <T> the generic type of the produced objects
 * @since 4.0
 */
public class DefaultResolverSetBasedObjectBuilder<T> extends ResolverSetBasedObjectBuilder<T> {

  private final Class<T> prototypeClass;
  private final ClassLoader prototypeClassClassLoader;

  public DefaultResolverSetBasedObjectBuilder(Class<T> prototypeClass,
                                              ResolverSet resolverSet,
                                              ExpressionManager expressionManager,
                                              MuleContext context) {
    super(prototypeClass, resolverSet, expressionManager, context);
    this.prototypeClass = prototypeClass;
    prototypeClassClassLoader = prototypeClass.getClassLoader();
  }

  @Override
  protected T instantiateObject() {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, prototypeClassClassLoader);
    try {
      return createInstance(prototypeClass);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + prototypeClass), e);
    } finally {
      setContextClassLoader(thread, prototypeClassClassLoader, currentClassLoader);
    }
  }
}

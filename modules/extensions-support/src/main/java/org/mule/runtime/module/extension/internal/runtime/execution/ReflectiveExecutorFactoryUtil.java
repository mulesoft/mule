/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;

/**
 * Utility class for Reflective Executors
 *
 * @since 4.1.6, 4.2.0
 */
final class ReflectiveExecutorFactoryUtil {

  private ReflectiveExecutorFactoryUtil() {

  }

  /**
   * Creates an instance of the given {@code methodOwnerClass} with the given {@code parameters}
   *
   * @param methodOwnerClass Class to instantiate
   * @param parameters       Parameters to inject in the object instance
   * @return a new {@code methodOwnerClass} instance.
   */
  static Object createDelegate(Class<?> methodOwnerClass, Map<String, Object> parameters) {
    DefaultObjectBuilder objectBuilder = new DefaultObjectBuilder<>(methodOwnerClass, new ReflectionCache());
    parameters.forEach((k, v) -> objectBuilder.addPropertyResolver(k, new StaticValueResolver<>(v)));
    Object delegate;
    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent();
      delegate = objectBuilder.build(from(initialiserEvent));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of operation class "
          + methodOwnerClass.getName()), e);
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
    return delegate;
  }
}

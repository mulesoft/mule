/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.container.api.MuleCoreExtensionDependency;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Discovers dependencies between {@link MuleCoreExtension} instances looking for methods annotated with
 * {@link MuleCoreExtensionDependency}
 */
public class ReflectionMuleCoreExtensionDependencyDiscoverer implements MuleCoreExtensionDependencyDiscoverer {

  @Override
  public List<LinkedMuleCoreExtensionDependency> findDependencies(MuleCoreExtension coreExtension) {
    List<LinkedMuleCoreExtensionDependency> result = new LinkedList<LinkedMuleCoreExtensionDependency>();

    final Method[] methods = coreExtension.getClass().getMethods();

    for (Method method : methods) {
      if (method.getAnnotation(MuleCoreExtensionDependency.class) != null) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 1) {
          if (MuleCoreExtension.class.isAssignableFrom(parameterTypes[0])) {
            final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency =
                new LinkedMuleCoreExtensionDependency((Class<? extends MuleCoreExtension>) parameterTypes[0], method);
            result.add(linkedMuleCoreExtensionDependency);
          }
        }
      }
    }

    return result;
  }
}

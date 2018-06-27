/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import static org.mule.runtime.api.exception.ExceptionHelper.getNonMuleException;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToInvokeLifecycle;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.internal.config.ExceptionHelper;
import org.mule.runtime.core.internal.lifecycle.LifecycleTransitionResult;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a configurable lifecycle phase. This is a default implementation of a 'generic phase' in that is can be configured
 * to represnt any phase. Instances of this phase can then be registered with a
 * {@link org.mule.runtime.core.api.lifecycle.LifecycleManager} and by used to enforce a lifecycle phase on an object. Usually,
 * Lifecycle phases have a fixed configuration in which case a specialisation of this class should be created that initialises its
 * configuration internally.
 * <p>
 * Note that this class and {@link LifecycleTransitionResult} both make assumptions about the
 * interfaces used - the return values and exceptions. These are, currently, that the return value is either void or
 * {@link LifecycleTransitionResult} and either 0 or 1 exceptions can be thrown which are
 * either {@link InstantiationException} or {@link LifecycleException}.
 *
 * @see LifecyclePhase
 */
public class DefaultLifecyclePhase implements LifecyclePhase {

  protected transient final Logger logger = LoggerFactory.getLogger(DefaultLifecyclePhase.class);

  private final Class<?> lifecycleClass;
  private final CheckedConsumer<Object> lifecycleInvoker;
  private Class<?>[] orderedLifecycleTypes;
  private Class<?>[] ignoredObjectTypes;
  private final String name;
  private Set<String> supportedPhases;

  public DefaultLifecyclePhase(String name, Class<?> lifecycleClass, CheckedConsumer<Object> lifecycleInvoker) {
    this.name = name;
    this.lifecycleClass = lifecycleClass;
    this.lifecycleInvoker = lifecycleInvoker;
  }

  @Override
  public List<Object> getSortedLifecycleObjects(Function<Class<?>, List<Object>> repository) {
    List<Object> objects = repository.apply(getLifecycleClass());

    if (orderedLifecycleTypes == null || orderedLifecycleTypes.length > 0) {
      List<Object>[] sorted = new List[orderedLifecycleTypes.length];
      for (Object object : objects) {
        boolean bucketed = false;
        for (int i = 0; i < orderedLifecycleTypes.length; i++) {
          if (orderedLifecycleTypes[i].isInstance(object)) {
            List<Object> bucket = sorted[i];
            if (bucket == null) {
              bucket = new LinkedList<>();
              sorted[i] = bucket;
            }
            bucket.add(object);
            bucketed = true;
            break;
          }
        }

        if (!bucketed) {
          throw new RuntimeException("HA!");
        }
      }

      objects = new LinkedList<>();
      for (List<Object> bucket : sorted) {
        if (bucket != null) {
          objects.addAll(bucket);
        }
      }
    }

    return objects;
  }

  protected boolean ignoreType(Class<?> type) {
    if (ignoredObjectTypes == null) {
      return false;
    } else {
      for (Class<?> ignoredType : ignoredObjectTypes) {
        if (ignoredType.isAssignableFrom(type)) {
          return true;
        }
      }
    }
    return false;
  }

  protected void setOrderedLifecycleTypes(Class<?>[] orderedLifecycleTypes) {
    this.orderedLifecycleTypes = orderedLifecycleTypes;
  }

  protected void setIgnoredObjectTypes(Class<?>[] ignorredObjectTypes) {
    this.ignoredObjectTypes = ignorredObjectTypes;
  }

  @Override
  public Class<?> getLifecycleClass() {
    return lifecycleClass;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void registerSupportedPhase(String phase) {
    if (supportedPhases == null) {
      supportedPhases = new HashSet<>();
    }
    supportedPhases.add(phase);
  }

  @Override
  public void applyLifecycle(Object o) throws LifecycleException {
    if (o == null) {
      return;
    }
    if (ignoreType(o.getClass())) {
      return;
    }
    if (!getLifecycleClass().isAssignableFrom(o.getClass())) {
      return;
    }
    if (o instanceof LifecycleStateEnabled) {
      // If an object has its own lifecycle manager "LifecycleStateEnabled" it
      // is possible that
      // its state can be controlled outside the registry i.e. via JMX, double
      // check here that we are
      // not calling the same lifecycle twice
      if (((LifecycleStateEnabled) o).getLifecycleState().isPhaseComplete(this.getName())) {
        return;
      } else if (!((LifecycleStateEnabled) o).getLifecycleState().isValidTransition(this.getName())) {
        return;
      }
    }
    try {
      lifecycleInvoker.accept(o);
    } catch (final Exception e) {
      Throwable t = ExceptionHelper.unwrap(e);

      if (t instanceof LifecycleException) {
        throw (LifecycleException) t;
      }

      // Need to get the cause of the MuleException so the LifecycleException wraps a non-mule exception
      throw new LifecycleException(failedToInvokeLifecycle(name, o), getNonMuleException(t), o);
    }
  }
}

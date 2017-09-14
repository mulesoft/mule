/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import static org.mule.runtime.api.exception.ExceptionHelper.getNonMuleException;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToInvokeLifecycle;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withName;

import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.lifecycle.LifecycleObject;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.runtime.core.internal.config.ExceptionHelper;
import org.mule.runtime.core.internal.lifecycle.LifecycleTransitionResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
  private Class<?> lifecycleClass;
  private final Method lifecycleMethod;
  private Set<LifecycleObject> orderedLifecycleObjects = new LinkedHashSet<>(6);
  private Class<?>[] ignorredObjectTypes;
  private final String name;
  private final String oppositeLifecyclePhase;
  private Set<String> supportedPhases;

  public DefaultLifecyclePhase(String name, Class<?> lifecycleClass, String oppositeLifecyclePhase) {
    this.name = name;
    this.lifecycleClass = lifecycleClass;
    Set<Method> lifecycleMethodsCandidate = getAllMethods(lifecycleClass, withName(name));
    lifecycleMethod = lifecycleMethodsCandidate.isEmpty() ? null : lifecycleMethodsCandidate.iterator().next();
    this.oppositeLifecyclePhase = oppositeLifecyclePhase;
  }

  /**
   * Subclasses can override this method to order <code>objects</code> before the lifecycle method is applied to them. This method
   * does not apply any special ordering to <code>objects</code>.
   *
   * @param objects
   * @param lo
   * @return List with ordered objects
   */
  protected List<?> sortLifecycleInstances(Collection<?> objects, LifecycleObject lo) {
    return new ArrayList<Object>(objects);
  }

  @Override
  public void addOrderedLifecycleObject(LifecycleObject lco) {
    orderedLifecycleObjects.add(lco);
  }

  @Override
  public void removeOrderedLifecycleObject(LifecycleObject lco) {
    orderedLifecycleObjects.remove(lco);
  }

  protected boolean ignoreType(Class<?> type) {
    if (ignorredObjectTypes == null) {
      return false;
    } else {
      for (Class<?> ignorredObjectType : ignorredObjectTypes) {
        if (ignorredObjectType.isAssignableFrom(type)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Set<LifecycleObject> getOrderedLifecycleObjects() {
    return orderedLifecycleObjects;
  }

  @Override
  public void setOrderedLifecycleObjects(Set<LifecycleObject> orderedLifecycleObjects) {
    this.orderedLifecycleObjects = orderedLifecycleObjects;
  }

  @Override
  public Class<?>[] getIgnoredObjectTypes() {
    return ignorredObjectTypes;
  }

  @Override
  public void setIgnoredObjectTypes(Class<?>[] ignorredObjectTypes) {
    this.ignorredObjectTypes = ignorredObjectTypes;
  }

  @Override
  public Class<?> getLifecycleClass() {
    return lifecycleClass;
  }

  @Override
  public void setLifecycleClass(Class<?> lifecycleClass) {
    this.lifecycleClass = lifecycleClass;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Set<String> getSupportedPhases() {
    return supportedPhases;
  }

  @Override
  public void setSupportedPhases(Set<String> supportedPhases) {
    this.supportedPhases = supportedPhases;
  }

  @Override
  public void registerSupportedPhase(String phase) {
    if (supportedPhases == null) {
      supportedPhases = new HashSet<>();
    }
    supportedPhases.add(phase);
  }

  @Override
  public boolean isPhaseSupported(String phase) {
    if (getSupportedPhases() == null) {
      return false;
    } else {
      if (getSupportedPhases().contains(ALL_PHASES)) {
        return true;
      } else {
        return getSupportedPhases().contains(phase);
      }
    }
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
      lifecycleMethod.invoke(o);
    } catch (final Exception e) {
      Throwable t = ExceptionHelper.unwrap(e);

      if (t instanceof LifecycleException) {
        throw (LifecycleException) t;
      }

      // Need to get the cause of the MuleException so the LifecycleException wraps a non-mule exception
      throw new LifecycleException(failedToInvokeLifecycle(lifecycleMethod.getName(), o),
                                   getNonMuleException(t), o);
    }
  }

  @Override
  public String getOppositeLifecyclePhase() {
    return oppositeLifecyclePhase;
  }
}

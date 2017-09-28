/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.Collection;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

/**
 * Utility class for performing lifecycle operations on objects, as long as they implement cooresponding annotations such as
 * {@link Initialisable}, {@link Startable}, {@link Stoppable}, {@link Disposable} or even {@link MuleContextAware}.
 * <p>
 * The {@link Optional} object container is also supported, in which case the operation will be evaluated on the value it holds or
 * not at all if the value is not present.
 *
 * @since 3.7.0
 */
public class LifecycleUtils {

  private LifecycleUtils() {}

  /**
   * Invokes {@link Initialisable#initialise()} on {@code object} if it implements the {@link Initialisable} interface.
   *
   * @param object the object you're trying to initialise
   * @throws InitialisationException
   */
  public static void initialiseIfNeeded(Object object) throws InitialisationException {
    object = unwrap(object);
    if (object instanceof Initialisable) {
      ((Initialisable) object).initialise();
    }
  }

  /**
   * The same as {@link #initialiseIfNeeded(Object)}, only that before checking for {@code object} being {@link Initialisable}, it
   * uses the given {@code muleContext} to perform further initialization and dependency injection on the {@code object}.
   * <p>
   * It checks if the {@code object} implements {@link MuleContextAware}, in which case it will invoke
   * {@link MuleContextAware#setMuleContext(MuleContext)} with the given {@code muleContext}.
   *
   * @param object the object you're trying to initialise
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   * @throws IllegalArgumentException if {@code MuleContext} is {@code null}
   */
  public static void initialiseIfNeeded(Object object, MuleContext muleContext) throws InitialisationException {
    initialiseIfNeeded(object, true, muleContext);
  }

  /**
   * The same as {@link #initialiseIfNeeded(Object)}, only that before checking for {@code object} being {@link Initialisable}, it
   * uses the given {@code muleContext} to perform further initialization.
   * <p>
   * It checks if the {@code object} implements {@link MuleContextAware}, in which case it will invoke
   * {@link MuleContextAware#setMuleContext(MuleContext)} with the given {@code muleContext}.
   * <p>
   * Also depending on the value of the {@code inject} argument, it will perform dependency injection on the {@code object}
   *
   * @param object the object you're trying to initialise
   * @param inject whether it should perform dependency injection on the {@code object} before actually initialising it
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   * @throws IllegalArgumentException if {@code MuleContext} is {@code null}
   */
  public static void initialiseIfNeeded(Object object, boolean inject, MuleContext muleContext) throws InitialisationException {
    checkArgument(muleContext != null, "muleContext cannot be null");
    object = unwrap(object);

    if (object == null) {
      return;
    }

    if (object instanceof MuleContextAware) {
      ((MuleContextAware) object).setMuleContext(muleContext);
    }

    if (inject) {
      try {
        muleContext.getInjector().inject(object);
      } catch (MuleException e) {
        I18nMessage message =
            createStaticMessage(format("Found exception trying to inject object of type '%s' on initialising phase",
                                       object.getClass().getName()));
        if (object instanceof Initialisable) {
          throw new InitialisationException(message, e, (Initialisable) object);
        }
        throw new MuleRuntimeException(message, e);
      }
    }

    initialiseIfNeeded(object);
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link #initialiseIfNeeded(Object)}
   *
   * @param objects the list of objects to be initialised
   * @throws InitialisationException
   */
  public static void initialiseIfNeeded(Collection<? extends Object> objects) throws InitialisationException {
    initialiseIfNeeded(objects, null);
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link #initialiseIfNeeded(Object, MuleContext)}
   *
   * @param objects the list of objects to be initialised
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   */
  public static void initialiseIfNeeded(Collection<? extends Object> objects, MuleContext muleContext)
      throws InitialisationException {
    initialiseIfNeeded(objects, true, muleContext);
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link #initialiseIfNeeded(Object, MuleContext)}
   * <p>
   * Also depending on the value of the {@code inject} argument, it will perform dependency injection on the {@code objects}
   *
   * @param objects the list of objects to be initialised
   * @param inject whether it should perform dependency injection on the {@code object} before actually initialising it
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   */
  public static void initialiseIfNeeded(Collection<? extends Object> objects, boolean inject, MuleContext muleContext)
      throws InitialisationException {
    for (Object object : objects) {
      initialiseIfNeeded(object, inject, muleContext);
    }
  }

  /**
   * Invokes {@link Startable#start()} on {@code object} if it implements the {@link Startable} interface
   *
   * @param object the object you're trying to start
   * @throws MuleException
   */
  public static void startIfNeeded(Object object) throws MuleException {
    object = unwrap(object);
    if (object instanceof Startable) {
      ((Startable) object).start();
    }
  }

  /**
   * For each item in the {@code objects} collection, it invokes the the {@link Startable#start()} if it implements the
   * {@link Startable} interface.
   *
   * @param objects the list of objects to be started
   * @throws MuleException
   */
  public static void startIfNeeded(Collection<? extends Object> objects) throws MuleException {
    doApplyPhase(Startable.PHASE_NAME, objects, null, null);
  }

  /**
   * For each item in the {@code objects} collection, it invokes the {@link Stoppable#stop()} if it implements the
   * {@link Stoppable} interface.
   *
   * @param objects the list of objects to be stopped
   * @throws MuleException
   */
  public static void stopIfNeeded(Collection<? extends Object> objects) throws MuleException {
    doApplyPhase(Stoppable.PHASE_NAME, objects, null, null);
  }

  /**
   * For each item in the {@code objects} collection, it invokes the {@link Stoppable#stop()} if it implements the
   * {@link Stoppable} interface.
   * <p>
   * This method is considered safe because it will not throw exception and the {@link Stoppable#stop()} method will be called on
   * all the {@code objects} regarding on any (or all) of them throwing exceptions. Any exceptions generated will be logged using
   * the provided {@code logger} and processing will continue
   *
   * @param objects the list of objects to be stopped
   * @param logger the {@link Logger} in which any exception found is to be logged
   */
  public static void safeStopIfNeeded(Collection<? extends Object> objects, Logger logger) {
    try {
      doApplyPhase(Stoppable.PHASE_NAME, objects, null, logger);
    } catch (Exception e) {
      logger.warn("Found unexpected exception during safe stop", e);
    }
  }

  /**
   * Invokes the {@link Stoppable#stop()} on {@code object} if it implements the {@link Stoppable} interface.
   *
   * @param object the object you're trying to stop
   * @throws MuleException
   */
  public static void stopIfNeeded(Object object) throws MuleException {
    object = unwrap(object);
    if (object instanceof Stoppable) {
      ((Stoppable) object).stop();
    }
  }

  /**
   * Invokes {@link Disposable#dispose()} on {@code object} if it implements the {@link Disposable} interface. If the dispose
   * operation fails, then the exception will be silently logged using the provided {@code logger}
   *
   * @param object the object you're trying to dispose
   */
  public static void disposeIfNeeded(Object object, Logger logger) {
    object = unwrap(object);
    if (object instanceof Disposable) {
      try {
        ((Disposable) object).dispose();
      } catch (Exception e) {
        logger.error("Exception found trying to dispose object. Shutdown will continue", e);
      }
    }
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link Disposable#dispose()} if it implements the
   * {@link Disposable} interface.
   * <p>
   * Per each dispose operation that fails, the exception will be silently logged using the provided {@code logger}
   *
   * @param objects the list of objects to be stopped
   * @throws MuleException
   */
  public static void disposeIfNeeded(Collection<? extends Object> objects, Logger logger) {
    try {
      doApplyPhase(Disposable.PHASE_NAME, objects, null, logger);
    } catch (Exception e) {
      logger.error("Exception found trying to dispose object. Shutdown will continue", e);
    }
  }

  /**
   * Verifies that the given {@code muleContext} is not stopped or in the process of stopping
   *
   * @param muleContext the {@link MuleContext} to test
   * @param errorMessage the message of the {@link Exception} to be thrown if the assertion fails
   * @throws IllegalStateException if the {@code muleContext} is stopped or stopping
   * @since 4.0
   */
  public static void assertNotStopping(MuleContext muleContext, String errorMessage) {
    if (muleContext.isStopped() || muleContext.isStopping()) {
      throw new IllegalStateException(errorMessage);
    }
  }

  /**
   * Sets an objects {@link MuleContext} if it implements {@link MuleContextAware}.
   *
   * @param object the object to inject the {@link MuleContext} into.
   * @param muleContext the {@link MuleContext} in which the object is defined.
   * @throws InitialisationException
   */
  public static void setMuleContextIfNeeded(Object object, MuleContext muleContext) {
    object = unwrap(object);
    if (object != null && object instanceof MuleContextAware) {
      ((MuleContextAware) object).setMuleContext(muleContext);
    }
  }

  /**
   * Sets an objects {@link MuleContext} if it implements {@link MuleContextAware}.
   *
   * @param objects the objects to inject the {@link MuleContext} into.
   * @param muleContext the {@link MuleContext} in which the object is defined.
   * @throws InitialisationException
   */
  public static void setMuleContextIfNeeded(Collection<? extends Object> objects, MuleContext muleContext) {
    objects.forEach(o -> setMuleContextIfNeeded(o, muleContext));
  }

  private static void doApplyPhase(String phase, Collection<? extends Object> objects, MuleContext muleContext, Logger logger)
      throws MuleException {
    if (CollectionUtils.isEmpty(objects)) {
      return;
    }

    for (Object object : objects) {
      object = unwrap(object);
      if (object == null) {
        continue;
      }

      try {
        if (Initialisable.PHASE_NAME.equals(phase)) {
          if (muleContext != null) {
            initialiseIfNeeded(object, muleContext);
          } else {
            initialiseIfNeeded(object);
          }
        } else if (Startable.PHASE_NAME.equals(phase)) {
          startIfNeeded(object);
        } else if (Stoppable.PHASE_NAME.equals(phase)) {
          stopIfNeeded(object);
        } else if (Disposable.PHASE_NAME.equals(phase) && object instanceof Disposable) {
          disposeIfNeeded(object, logger);
        }
      } catch (MuleException e) {
        if (logger != null) {
          logger.error(format("Could not apply %s phase on object of class %s", phase, object.getClass().getName()), e);
        } else {
          throw e;
        }
      }
    }
  }

  private static Object unwrap(Object value) {
    if (value instanceof Optional) {
      return ((Optional) value).orElse(null);
    }

    return value;
  }
}

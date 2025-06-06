/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.Collection;
import java.util.Optional;

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
   * @param object      the object you're trying to initialise
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   * @throws IllegalArgumentException if {@code MuleContext} is {@code null}
   * @deprecated Use {@link #initialiseIfNeeded(Object, Injector)} instead.
   */
  @Deprecated(since = "4.10", forRemoval = true)
  public static void initialiseIfNeeded(Object object, MuleContext muleContext) throws InitialisationException {
    initialiseIfNeeded(object, true, muleContext);
  }

  /**
   * The same as {@link #initialiseIfNeeded(Object)}, only that before checking for {@code object} being {@link Initialisable}, it
   * uses the given {@code injector} to perform further initialization and dependency injection on the {@code object}.
   *
   * @param object   the object you're trying to initialise
   * @param injector an {@link Injector} for the current artifact context
   * @throws InitialisationException
   * @throws IllegalArgumentException if {@code injector} is {@code null}
   * @since 4.10
   */
  public static void initialiseIfNeeded(Object object, Injector injector) throws InitialisationException {
    requireNonNull(injector, "injector cannot be null");
    object = unwrap(object);

    if (object == null) {
      return;
    }

    doInject(object, injector);
    initialiseIfNeeded(object);
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
   * @param object      the object you're trying to initialise
   * @param inject      whether it should perform dependency injection on the {@code object} before actually initialising it
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   * @throws IllegalArgumentException if {@code MuleContext} is {@code null}
   * @deprecated Use {@link #initialiseIfNeeded(Object, Injector)} instead.
   */
  @Deprecated(since = "4.10", forRemoval = true)
  public static void initialiseIfNeeded(Object object, boolean inject, MuleContext muleContext) throws InitialisationException {
    requireNonNull(muleContext, "muleContext cannot be null");
    object = setMuleContextIfNeededFluent(object, muleContext);
    if (inject) {
      final var injector = muleContext.getInjector();
      initialiseIfNeeded(object, injector);
    } else {
      initialiseIfNeeded(object);
    }
  }

  private static void doInject(Object object, final Injector injector) throws InitialisationException {
    try {
      injector.inject(object);
    } catch (MuleException e) {
      I18nMessage message =
          createStaticMessage(format("Found exception trying to inject object of type '%s' on initialising phase",
                                     object.getClass().getName()));
      if (object instanceof Initialisable initialisable) {
        throw new InitialisationException(message, e, initialisable);
      }
      throw new MuleRuntimeException(message, e);
    }
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link #initialiseIfNeeded(Object)}
   *
   * @param objects the list of objects to be initialised
   * @throws InitialisationException
   */
  public static void initialiseIfNeeded(Collection<? extends Object> objects) throws InitialisationException {
    for (Object object : objects) {
      initialiseIfNeeded(object);
    }
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link #initialiseIfNeeded(Object, Injector)}
   *
   * @param objects  the list of objects to be initialised
   * @param injector an {@link Injector} for the current artifact context
   * @throws InitialisationException
   * @since 4.10
   */
  public static void initialiseIfNeeded(Collection<? extends Object> objects, Injector injector)
      throws InitialisationException {
    for (Object object : objects) {
      initialiseIfNeeded(object, injector);
    }
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link #initialiseIfNeeded(Object, MuleContext)}
   *
   * @param objects     the list of objects to be initialised
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   * @deprecated Use {@link #initialiseIfNeeded(Collection, Injector) instead.
   */
  @Deprecated(since = "4.10", forRemoval = true)
  public static void initialiseIfNeeded(Collection<? extends Object> objects, MuleContext muleContext)
      throws InitialisationException {
    initialiseIfNeeded(objects, true, muleContext);
  }

  /**
   * For each item in the {@code objects} collection, it invokes {@link #initialiseIfNeeded(Object, MuleContext)}
   * <p>
   * Also depending on the value of the {@code inject} argument, it will perform dependency injection on the {@code objects}
   *
   * @param objects     the list of objects to be initialised
   * @param inject      whether it should perform dependency injection on the {@code object} before actually initialising it
   * @param muleContext a {@link MuleContext}
   * @throws InitialisationException
   * @deprecated Use {@link #initialiseIfNeeded(Collection, Injector) instead.
   */
  @Deprecated(since = "4.10", forRemoval = true)
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
    if (object instanceof Startable startable) {
      startable.start();
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
   * @param logger  the {@link Logger} in which any exception found is to be logged
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
    if (object instanceof Stoppable stoppable) {
      stoppable.stop();
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
    if (object instanceof Disposable disposable) {
      try {
        disposable.dispose();
      } catch (Exception e) {
        if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
          throw e;
        } else {
          logger.error("Exception found trying to dispose object. Shutdown will continue", e);
        }
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
   * @param muleContext  the {@link MuleContext} to test
   * @param errorMessage the message of the {@link Exception} to be thrown if the assertion fails
   * @throws IllegalStateException if the {@code muleContext} is stopped or stopping
   * @since 4.0
   * @deprecated Use {@link #assertNotStopping(LifecycleState, String)} instead.
   */
  @Deprecated(since = "4.10", forRemoval = true)
  public static void assertNotStopping(MuleContext muleContext, String errorMessage) {
    if (muleContext.isStopping() || (muleContext.isStopped() && !muleContext.isStarting())) {
      throw new IllegalStateException(errorMessage);
    }
  }

  /**
   * Verifies that the given {@code deploymentLifecycleState} is not stopped or in the process of stopping
   *
   * @param deploymentLifecycleState the {@link LifecycleState} of the deployment to test
   * @param errorMessage             the message of the {@link Exception} to be thrown if the assertion fails
   * @throws IllegalStateException if the {@code deploymentLifecycleState} is stopped or stopping
   * @since 4.10
   */
  public static void assertNotStopping(LifecycleState deploymentLifecycleState, String errorMessage) {
    if (deploymentLifecycleState.isStopping()
        || (deploymentLifecycleState.isStopped() && !deploymentLifecycleState.isStarting())) {
      throw new IllegalStateException(errorMessage);
    }
  }

  /**
   * Checks if the {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_LAZY_INIT_DEPLOYMENT_PROPERTY} property
   * has been set on the given {@code properties}.
   * <p>
   * If {@code properties} is {@code null} then {@code false} is returned.
   *
   * @param properties the current properties
   * @return Whether the lazy init property has been set
   * @since 4.3.0
   *
   * @deprecated since 4.4, do not rely on deployment properties at runtime.
   */
  @Deprecated
  public static boolean isLazyInitMode(ConfigurationProperties properties) {
    return properties != null && properties.resolveBooleanProperty(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY).orElse(false);
  }

  /**
   * Checks if the
   * {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY}
   * property has been set on the given {@code properties}.
   * <p>
   * If {@code properties} is {@code null} then {@code false} is returned.
   *
   * @param properties the current properties
   * @return Whether the DSL declaration validation property has been set
   * @since 4.3.0
   *
   * @deprecated since 4.4, do not rely on deployment properties at runtime.
   */
  @Deprecated
  public static boolean isDslDeclarationValidationEnabled(ConfigurationProperties properties) {
    return properties != null
        && properties.resolveBooleanProperty(MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY).orElse(false);
  }

  /**
   * Sets an objects {@link MuleContext} if it implements {@link MuleContextAware}.
   *
   * @param object      the object to inject the {@link MuleContext} into.
   * @param muleContext the {@link MuleContext} in which the object is defined.
   * @return {@code object} unwrapped
   * @throws InitialisationException
   */
  public static Object setMuleContextIfNeededFluent(Object object, MuleContext muleContext) {
    object = unwrap(object);
    if (object != null && object instanceof MuleContextAware mca) {
      mca.setMuleContext(muleContext);
    }
    return object;
  }

  /**
   * Sets an objects {@link MuleContext} if it implements {@link MuleContextAware}.
   *
   * @param object      the object to inject the {@link MuleContext} into.
   * @param muleContext the {@link MuleContext} in which the object is defined.
   * @throws InitialisationException
   */
  public static void setMuleContextIfNeeded(Object object, MuleContext muleContext) {
    object = unwrap(object);
    if (object != null && object instanceof MuleContextAware mca) {
      mca.setMuleContext(muleContext);
    }
  }

  /**
   * Sets an objects {@link MuleContext} if it implements {@link MuleContextAware}.
   *
   * @param objects     the objects to inject the {@link MuleContext} into.
   * @param muleContext the {@link MuleContext} in which the object is defined.
   * @throws InitialisationException
   */
  public static void setMuleContextIfNeeded(Collection<? extends Object> objects, MuleContext muleContext) {
    objects.forEach(o -> setMuleContextIfNeeded(o, muleContext));
  }

  private static void doApplyPhase(String phase, Collection<? extends Object> objects, MuleContext muleContext, Logger logger)
      throws MuleException {
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
    if (value instanceof Optional opt) {
      return opt.orElse(null);
    }

    return value;
  }
}

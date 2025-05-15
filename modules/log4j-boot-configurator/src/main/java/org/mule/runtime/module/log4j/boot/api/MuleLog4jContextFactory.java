/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.boot.api;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.apache.logging.log4j.LogManager.setFactory;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.log4j.boot.internal.ContextSelectorWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.lmax.disruptor.ExceptionHandler;

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

/**
 * <p>
 * Implementation of {@link LoggerContextFactory} which acts as the bootstrap for mule's logging mechanism.
 * </p>
 * <p>
 * It sets {@link XmlConfigurationFactory} as the only available {@link ConfigurationFactory}
 * </p>
 * <p>
 * It sets {@link AsyncLoggerExceptionHandler} as the {@link ExceptionHandler} for failing async loggers.
 * </p>
 * <p>
 * It also makes the {@link ContextSelector} dynamically configurable through
 * {@link #setContextSelector(ContextSelector, Consumer)}.
 * </p>
 * <p>
 * Other than that, it's pretty much a copy paste of {@link Log4jContextFactory}, due to that classes' lack of extensibility.
 * </p>
 * <p>
 * By forcing {@link XmlConfigurationFactory} as the only available {@link ConfigurationFactory} we're disabling log4j2's ability
 * to take json and yaml configurations. This is so because those configuration factories depend on versions of the jackson
 * libraries which would cause conflict with the ones in mule.
 * </p>
 * <p>
 * TODO: Upgrade the jackson libraries bundled with mule so that this restriction can be lifted off.
 * </p>
 * <p>
 * This class also implements {@link ShutdownCallbackRegistry} to avoid default behaviour.
 * </p>
 *
 * @since 4.5
 */
public class MuleLog4jContextFactory extends Log4jContextFactory implements ShutdownCallbackRegistry {

  private static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
  private static final String DEFAULT_LOG_CONFIGURATION_FACTORY = XmlConfigurationFactory.class.getName();
  private static final String ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY = "AsyncLoggerConfig.ExceptionHandler";
  private static final String DEFAULT_ASYNC_LOGGER_EXCEPTION_HANDLER = AsyncLoggerExceptionHandler.class.getName();

  /**
   * Creates a new instance and sets it as the factory of the {@link org.apache.logging.log4j.LogManager}.
   *
   * @return the created {@link MuleLog4jContextFactory}.
   */
  public static MuleLog4jContextFactory createAndInstall() {
    // We need to force the creation of a logger before we can change the manager factory.
    // This is because if not, any logger that will be acquired by MuleLog4jContextFactory code
    // will fail since it will try to use a null factory.
    getLogger("triggerDefaultFactoryCreation");
    // We need to set this property so log4j uses the same context factory everywhere
    setProperty("log4j2.loggerContextFactory", MuleLog4jContextFactory.class.getName());
    MuleLog4jContextFactory log4jContextFactory = new MuleLog4jContextFactory();
    setFactory(log4jContextFactory);
    return log4jContextFactory;
  }

  /**
   * Log4j tries to instantiate this class using a default constructor.
   */
  public MuleLog4jContextFactory() {
    super(new ContextSelectorWrapper(), new MuleShutdownCallbackRegistry());
    initialise();
  }

  /**
   * Initializes using a {@code contextSelector}
   *
   * @param contextSelector a {@link ContextSelector}
   */
  public MuleLog4jContextFactory(ContextSelector contextSelector, Consumer<ContextSelector> disposer) {
    super(new ContextSelectorWrapper(contextSelector, disposer), new MuleShutdownCallbackRegistry());
    initialise();
  }

  /**
   * Changes the {@link ContextSelector}.
   *
   * @param contextSelector a {@link ContextSelector}
   */
  public void setContextSelector(ContextSelector contextSelector, Consumer<ContextSelector> disposer) {
    ContextSelector selector = getSelector();
    if (selector instanceof ContextSelectorWrapper) {
      ((ContextSelectorWrapper) selector).setDelegate(contextSelector, disposer);
    }
  }

  protected void initialise() {
    setupConfigurationFactory();
    setupAsyncLoggerExceptionHandler();
  }

  private void setupConfigurationFactory() {
    setProperty(LOG_CONFIGURATION_FACTORY_PROPERTY, DEFAULT_LOG_CONFIGURATION_FACTORY);
  }

  private void setupAsyncLoggerExceptionHandler() {
    String handler = getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY);
    if (isBlank(handler)) {
      setProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY, DEFAULT_ASYNC_LOGGER_EXCEPTION_HANDLER);
    }
  }

  public void dispose() {
    ContextSelector selector = getSelector();
    if (selector instanceof ContextSelectorWrapper) {
      ((ContextSelectorWrapper) selector).disposeDelegate();
    }
    MuleShutdownCallbackRegistry shutdownCallbackRegistry = (MuleShutdownCallbackRegistry) getShutdownCallbackRegistry();
    shutdownCallbackRegistry.dispose();
  }

  private static class MuleShutdownCallbackRegistry implements ShutdownCallbackRegistry {

    private final ExecutorService executorService =
        newCachedThreadPool(runnable -> new Thread(runnable, "[MuleRuntime].log4j.shudownhook"));

    private final List<Runnable> hooks = new ArrayList<>();

    @Override
    public Cancellable addShutdownCallback(Runnable callback) {
      hooks.add(callback);
      return new Cancellable() {

        @Override
        public void cancel() {
          hooks.remove(callback);
        }

        @Override
        public void run() {
          callback.run();
        }
      };
    }

    public void dispose() {
      for (Runnable hook : new ArrayList<>(hooks)) {
        executorService.submit(hook);
      }
      try {
        executorService.awaitTermination(1000, MILLISECONDS);
      } catch (InterruptedException e) {
        // Nothing to do, not even log since we are shutting down the logger
      }
      executorService.shutdownNow();
    }
  }

  public static boolean isBlank(CharSequence cs) {
    int strLen = length(cs);
    if (strLen == 0) {
      return true;
    } else {
      for (int i = 0; i < strLen; ++i) {
        if (!Character.isWhitespace(cs.charAt(i))) {
          return false;
        }
      }

      return true;
    }
  }

  public static int length(CharSequence cs) {
    return cs == null ? 0 : cs.length();
  }

}

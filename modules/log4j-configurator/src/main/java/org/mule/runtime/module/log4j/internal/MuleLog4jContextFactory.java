/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LOG_SEPARATION_DISABLED;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.Executors.newCachedThreadPool;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.module.log4j.api.AsyncLoggerExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.ExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

/**
 * Implementation of {@link LoggerContextFactory} which acts as the bootstrap for mule's logging mechanism.
 * <p/>
 * It forces {@link ArtifactAwareContextSelector} as the only selector, {@link XmlConfigurationFactory} as the only available
 * {@link ConfigurationFactory}, and sets {@link AsyncLoggerExceptionHandler} as the {@link ExceptionHandler} for failing async
 * loggers.
 * <p/>
 * Other than that, it's pretty much a copy paste of {@link Log4jContextFactory}, due to that classes' lack of extensibility.
 * <p/>
 * By forcing {@link XmlConfigurationFactory} as the only available {@link ConfigurationFactory} we're disabling log4j2's ability
 * to take json and yaml configurations. This is so because those configuration factories depend on versions of the jackson
 * libraries which would cause conflict with the ones in mule. TODO: Upgrade the jackson libraries bundled with mule so that this
 * restriction can be lifted off
 * <p/>
 * This class also implements {@link ShutdownCallbackRegistry} to avoid default behaviour which is
 *
 * @since 4.5
 */
public class MuleLog4jContextFactory extends Log4jContextFactory implements Disposable, ShutdownCallbackRegistry {

  public static final boolean LOG_SEPARATION_ENABLED = getProperty(MULE_LOG_SEPARATION_DISABLED) == null;

  private static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
  private static final String DEFAULT_LOG_CONFIGURATION_FACTORY = XmlConfigurationFactory.class.getName();
  private static final String ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY = "AsyncLoggerConfig.ExceptionHandler";
  private static final String DEFAULT_ASYNC_LOGGER_EXCEPTION_HANDLER = AsyncLoggerExceptionHandler.class.getName();

  /**
   * Initializes using a {@link ArtifactAwareContextSelector}
   * <p>
   * Log4j tries to instantiate this class using a default constructor.
   */
  public MuleLog4jContextFactory() {
    super(new ContextSelectorWrapper(), new MuleShutdownCallbackRegistry());
    initialise();
  }

  /**
   * Initializes using {@code contextSelector}
   *
   * @param contextSelector a {@link ContextSelector}
   */
  public MuleLog4jContextFactory(ContextSelector contextSelector) {
    super(new ContextSelectorWrapper(contextSelector), new MuleShutdownCallbackRegistry());
    initialise();
  }

  public void setContextSelector(ContextSelector contextSelector) {
    ContextSelector selector = getSelector();
    if (selector instanceof ContextSelectorWrapper) {
      ((ContextSelectorWrapper) selector).setDelegate(contextSelector);
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
    String handler = System.getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY);
    if (StringUtils.isBlank(handler)) {
      setProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY, DEFAULT_ASYNC_LOGGER_EXCEPTION_HANDLER);
    }
  }

  @Override
  public void dispose() {
    ContextSelector selector = getSelector();
    if (selector instanceof Disposable) {
      ((Disposable) selector).dispose();
    }
    MuleShutdownCallbackRegistry shutdownCallbackRegistry = (MuleShutdownCallbackRegistry) getShutdownCallbackRegistry();
    shutdownCallbackRegistry.dispose();
  }

  private static class MuleShutdownCallbackRegistry implements ShutdownCallbackRegistry, Disposable {

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

    @Override
    public void dispose() {
      for (Runnable hook : new ArrayList<>(hooks)) {
        executorService.submit(hook);
      }
      try {
        executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // Nothing to do, not even log since we are shutting down the logger
      }
      executorService.shutdownNow();
    }
  }

}

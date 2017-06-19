/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.util.concurrent.Executors.newCachedThreadPool;
import org.mule.runtime.api.lifecycle.Disposable;

import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;

/**
 * Implementation of {@link org.apache.logging.log4j.spi.LoggerContextFactory} which acts as the bootstrap for mule's logging
 * mechanism.
 * <p/>
 * It forces {@link org.mule.runtime.module.launcher.log4j2.ArtifactAwareContextSelector} as the only selector,
 * {@link org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory} as the only available
 * {@link org.apache.logging.log4j.core.config.ConfigurationFactory}, and sets
 * {@link org.mule.runtime.module.launcher.log4j2.AsyncLoggerExceptionHandler} as the {@link com.lmax.disruptor.ExceptionHandler}
 * for failing async loggers.
 * <p/>
 * Other than that, it's pretty much a copy paste of {@link org.apache.logging.log4j.core.impl.Log4jContextFactory}, due to that
 * classes' lack of extensibility.
 * <p/>
 * By forcing {@link org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory} as the only available
 * {@link org.apache.logging.log4j.core.config.ConfigurationFactory} we're disabling log4j2's ability to take json and yaml
 * configurations. This is so because those configuration factories depend on versions of the jackson libraries which would cause
 * conflict with the ones in mule. TODO: Upgrade the jackson libraries bundled with mule so that this restriction can be lifted
 * off
 * <p/>
 * This class also implements {@link ShutdownCallbackRegistry} to avoid default behaviour which is
 *
 * @since 3.6.0
 */
public class MuleLog4jContextFactory extends Log4jContextFactory implements Disposable, ShutdownCallbackRegistry {

  private static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
  private static final String DEFAULT_LOG_CONFIGURATION_FACTORY = XmlConfigurationFactory.class.getName();
  private static final String ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY = "AsyncLoggerConfig.ExceptionHandler";
  private static final String DEFAULT_ASYNC_LOGGER_EXCEPTION_HANLDER = AsyncLoggerExceptionHandler.class.getName();

  /**
   * Initializes using a {@link ArtifactAwareContextSelector}
   */
  public MuleLog4jContextFactory() {
    this(new ArtifactAwareContextSelector());

  }

  /**
   * Initializes using {@code contextSelector}
   *
   * @param contextSelector a {@link ContextSelector}
   */
  public MuleLog4jContextFactory(ContextSelector contextSelector) {
    super(contextSelector, new MuleShutdownCallbackRegistry());
    initialise();
  }

  protected void initialise() {
    setupConfigurationFactory();
    setupAsyncLoggerExceptionHandler();
  }

  private void setupConfigurationFactory() {
    System.setProperty(LOG_CONFIGURATION_FACTORY_PROPERTY, DEFAULT_LOG_CONFIGURATION_FACTORY);
  }

  private void setupAsyncLoggerExceptionHandler() {
    String handler = System.getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY);
    if (StringUtils.isBlank(handler)) {
      System.setProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY, DEFAULT_ASYNC_LOGGER_EXCEPTION_HANLDER);
    }
  }

  @Override
  public void dispose() {
    ((ArtifactAwareContextSelector) getSelector()).dispose();
    MuleShutdownCallbackRegistry shutdownCallbackRegistry = (MuleShutdownCallbackRegistry) getShutdownCallbackRegistry();
    shutdownCallbackRegistry.dispose();
  }

  private static class MuleShutdownCallbackRegistry implements ShutdownCallbackRegistry, Disposable {

    private ExecutorService executorService =
        newCachedThreadPool(runnable -> new Thread(runnable, "[MuleRuntime].log4j.shudownhook"));

    private List<Runnable> hooks = new ArrayList<>();

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
      for (Runnable hook : hooks) {
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

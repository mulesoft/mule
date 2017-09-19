/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

/**
 * Configuration info. which can be set when creating the MuleContext but becomes immutable after startup.
 * TODO MULE-13121 Cleanup MuleConfiguration removing redundant config in Mule 4
 */
public interface MuleConfiguration {

  int getDefaultResponseTimeout();

  String getWorkingDirectory();

  String getMuleHomeDirectory();

  int getDefaultTransactionTimeout();

  boolean isClientMode();

  String getDefaultEncoding();

  String getId();

  String getDomainId();

  String getSystemModelType();

  String getSystemName();

  boolean isAutoWrapMessageAwareTransform();

  boolean isCacheMessageAsBytes();

  boolean isEnableStreaming();

  boolean isValidateExpressions();

  @Deprecated
  int getDefaultQueueTimeout();

  /**
   * @return The graceful shutdown timeout (in millis) used when stopping a mule application cleanly without message loss.
   */
  long getShutdownTimeout();

  /**
   * The approximated maximum space in megabytes used by the transaction log files for transactional persistent queues.
   *
   * Take into account that this number applies both to the set of transaction log files for XA and for local transactions. If
   * both type of transactions are used then the approximated maximum space used will be twice the configured value.
   *
   * @return the approximated maximum space in disk that the transactions logs can use in megabytes.
   */
  int getMaxQueueTransactionFilesSizeInMegabytes();

  /**
   * A container mode implies multiple Mule apps running. When true, Mule changes behavior in some areas, e.g.:
   * <ul>
   * <li>Splash screens</li>
   * <li>Thread names have app name in the prefix to guarantee uniqueness</li>
   * </ul>
   * etc.
   *
   * Note that e.g. a WAR-embedded Mule will run in container mode, but will still be considerd embedded for management purposes.
   *
   * @see #isStandalone()
   */
  boolean isContainerMode();

  /**
   * Try to guess if we're embedded. If "mule.home" JVM property has been set, then we've been started via Mule script and can
   * assume we're running standalone. Otherwise (no property set), Mule has been started via a different mechanism.
   * <p/>
   * A standalone Mule is always considered running in 'container' mode.
   *
   * @see #isContainerMode()
   */
  boolean isStandalone();

  /**
   * @return default error handler to be used on flows and services if there's no error handler configured explicitly.
   */
  String getDefaultErrorHandlerName();

  boolean isDisableTimeouts();

  /**
   * @param extensionType class instance of the extension type
   * @param <T> type of the extension
   * @return extension configured of type extensionType, if there's no such extension then null.
   */
  <T> T getExtension(final Class<T> extensionType);

  /**
   * Returns the default instance of {@link ObjectSerializer} to be used. This instance will be accessible through
   * {@link MuleContext#getObjectSerializer()}.
   * <p/>
   * If not provided, if defaults to an instance of {@link ObjectSerializer}
   *
   * @return a {@link ObjectSerializer}
   * @since 3.7.0
   */
  ObjectSerializer getDefaultObjectSerializer();

  /**
   * The {@link ProcessingStrategyFactory factory} of the default {@link ProcessingStrategy} to be used by all {@link Flow}s which
   * doesn't specify otherwise
   *
   * @return a {@link ProcessingStrategyFactory}
   * @since 3.7.0
   */
  ProcessingStrategyFactory getDefaultProcessingStrategyFactory();

  /**
   * @return The default {@link DynamicConfigExpiration} that will be used in any dynamic config which doesn't specify its own
   */
  DynamicConfigExpiration getDynamicConfigExpiration();

}

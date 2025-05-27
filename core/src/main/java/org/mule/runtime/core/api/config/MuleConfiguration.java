/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.Optional;

/**
 * Configuration info. which can be set when creating the MuleContext but becomes immutable after startup.
 * <p/>
 * TODO MULE-13121 Cleanup MuleConfiguration removing redundant config in Mule 4
 */
@NoImplement
public interface MuleConfiguration {

  int getDefaultResponseTimeout();

  String getWorkingDirectory();

  String getMuleHomeDirectory();

  int getDefaultTransactionTimeout();

  boolean isClientMode();

  /**
   * @return
   * @deprecated use {@link org.mule.runtime.api.config.ArtifactEncoding} instead.
   */
  @Deprecated
  String getDefaultEncoding();

  String getId();

  String getDomainId();

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated
  String getSystemModelType();

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated
  String getSystemName();

  boolean isAutoWrapMessageAwareTransform();

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated
  boolean isCacheMessageAsBytes();

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated
  boolean isEnableStreaming();

  boolean isValidateExpressions();

  /**
   * @return Whether this context was deployed in lazy init mode
   * @since 4.3.0
   * @deprecated Since 4.4 this will always return {@code false}. Components/artifacts need not to know if the deployment is lazy.
   */
  @Deprecated
  boolean isLazyInit();

  @Deprecated
  int getDefaultQueueTimeout();

  /**
   * @return The graceful shutdown timeout (in millis) used when stopping a mule application cleanly without message loss.
   */
  long getShutdownTimeout();

  /**
   * The approximated maximum space in megabytes used by the transaction log files for transactional persistent queues.
   * <p>
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
   * <p>
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
   * @param <T>           type of the extension
   * @return extension configured of type extensionType, if there's no such extension then null.
   */
  <T> T getExtension(final Class<T> extensionType);

  /**
   * Returns the default instance of {@link ObjectSerializer} to be used. This instance will be accessible through injection.
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

  /**
   * @return whether streaming iterable items should follow the iterable repeatability
   * @since 4.3
   */
  boolean isInheritIterableRepeatability();


  /**
   * @return The {@link MuleVersion} that the application has configured.
   *
   * @since 4.4.0
   */
  Optional<MuleVersion> getMinMuleVersion();

  /**
   * @return the default Correlation ID generator for every source. Empty value will be returned if internal mule correlation id
   *         must be kept
   *
   * @since 4.4.0
   */
  Optional<CorrelationIdGenerator> getDefaultCorrelationIdGenerator();

  /**
   * @return Optionally returns the {@link ArtifactCoordinates} for the deployed app
   * @since 4.5.0
   */
  Optional<ArtifactCoordinates> getArtifactCoordinates();
}

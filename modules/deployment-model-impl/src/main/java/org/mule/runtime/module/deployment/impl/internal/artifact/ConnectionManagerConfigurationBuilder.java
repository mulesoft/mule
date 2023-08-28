/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.isConfigLess;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.withArtifactMuleContext;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.internal.connection.CompositeConnectionManager;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.DelegateConnectionManagerAdapter;
import org.mule.runtime.deployment.model.api.DeployableArtifact;

/**
 * {@link ConfigurationBuilder} implementation which enriches the {@link MuleContext}'s registry injecting the correspondent
 * {@link ConnectionManager}
 *
 * @since 4.0
 */
public class ConnectionManagerConfigurationBuilder implements ConfigurationBuilder {

  private final CheckedConsumer<MuleContext> muleContextConfigurer;

  ConnectionManagerConfigurationBuilder(DeployableArtifact parentArtifact) {
    checkNotNull(parentArtifact, "'parentArtifact' can't be null");

    muleContextConfigurer = muleContext -> withArtifactMuleContext(parentArtifact, parentContext -> {
      if (isConfigLess(parentArtifact)) {
        registerDefaultConnectionManager(muleContext);
      } else {
        ConnectionManagerAdapter parentConnectionManager = lookupObject(parentContext, OBJECT_CONNECTION_MANAGER);
        if (parentConnectionManager != null) {
          ConnectionManager connectionManager =
              new CompositeConnectionManager(new DelegateConnectionManagerAdapter(muleContext), parentConnectionManager);
          registerConnectionManager(muleContext, connectionManager);
        } else {
          registerDefaultConnectionManager(muleContext);
        }
      }
    });
  }

  ConnectionManagerConfigurationBuilder() {
    muleContextConfigurer = this::registerDefaultConnectionManager;
  }

  @Override
  public void configure(MuleContext muleContext) throws ConfigurationException {
    try {
      muleContextConfigurer.acceptChecked(muleContext);
    } catch (Throwable e) {
      throw new ConfigurationException(createStaticMessage("An error occurred trying to register the Mule Connection Manager"),
                                       e);
    }
  }

  private void registerDefaultConnectionManager(MuleContext muleContext) {
    registerConnectionManager(muleContext, new DelegateConnectionManagerAdapter(muleContext));
  }

  private void registerConnectionManager(MuleContext muleContext, ConnectionManager connectionManager) {
    muleContext.getCustomizationService().overrideDefaultServiceImpl(OBJECT_CONNECTION_MANAGER, connectionManager);
  }

  @Override
  public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
    // Nothing to do
  }
}

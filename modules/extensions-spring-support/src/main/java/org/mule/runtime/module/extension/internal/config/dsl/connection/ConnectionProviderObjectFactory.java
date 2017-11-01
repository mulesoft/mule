/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.connection;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthManager;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.soap.internal.loader.property.SoapExtensionModelProperty;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.SoapConnectionProviderObjectBuilder;

import java.util.concurrent.Callable;

import javax.inject.Inject;

/**
 * A {@link AbstractExtensionObjectFactory} that produces {@link ConnectionProviderResolver} instances
 *
 * @since 4.0
 */
public class ConnectionProviderObjectFactory extends AbstractExtensionObjectFactory<ConnectionProviderResolver> {

  private final ConnectionProviderModel providerModel;
  private final ExtensionModel extensionModel;
  private final ExtensionsOAuthManager oauthManager;

  private PoolingProfile poolingProfile = null;
  private ReconnectionConfig reconnectionConfig = ReconnectionConfig.getDefault();

  @Inject
  private MuleContext muleContext;

  public ConnectionProviderObjectFactory(ConnectionProviderModel providerModel,
                                         ExtensionModel extensionModel,
                                         ExtensionsOAuthManager oauthManager,
                                         MuleContext muleContext) {
    super(muleContext);
    this.providerModel = providerModel;
    this.extensionModel = extensionModel;
    this.oauthManager = oauthManager;
  }

  @Override
  public ConnectionProviderResolver doGetObject() throws Exception {
    Callable<ResolverSet> callable = () -> getParametersResolver().getParametersAsHashedResolverSet(providerModel, muleContext);
    ResolverSet resolverSet = withContextClassLoader(getClassLoader(extensionModel), callable);

    ConnectionProviderObjectBuilder builder;
    if (extensionModel.getModelProperty(SoapExtensionModelProperty.class).isPresent()) {
      builder = new SoapConnectionProviderObjectBuilder(providerModel, resolverSet, poolingProfile,
                                                        reconnectionConfig,
                                                        extensionModel,
                                                        muleContext);
    } else if (providerModel.getModelProperty(OAuthModelProperty.class).isPresent()) {
      builder = new OAuthConnectionProviderObjectBuilder(providerModel, resolverSet, poolingProfile,
                                                         reconnectionConfig,
                                                         oauthManager, extensionModel,
                                                         muleContext);
    } else {
      builder = new DefaultConnectionProviderObjectBuilder(providerModel, resolverSet, poolingProfile,
                                                           reconnectionConfig,
                                                           extensionModel,
                                                           muleContext);
    }

    return new ConnectionProviderResolver<>(builder, resolverSet, muleContext);
  }

  public void setPoolingProfile(PoolingProfile poolingProfile) {
    this.poolingProfile = poolingProfile;
  }

  public void setReconnectionConfig(ReconnectionConfig reconnectionConfig) {
    this.reconnectionConfig = reconnectionConfig;
  }
}

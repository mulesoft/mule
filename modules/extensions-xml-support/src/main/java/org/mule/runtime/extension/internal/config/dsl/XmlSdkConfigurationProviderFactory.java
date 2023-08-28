/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.extension.internal.config.dsl;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.exception.RequiredParameterNotSetException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class XmlSdkConfigurationProviderFactory extends AbstractExtensionObjectFactory<ConfigurationProvider> {

  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final Registry registry;

  private final LazyValue<String> configName = new LazyValue<>(this::getName);

  public XmlSdkConfigurationProviderFactory(ExtensionModel extensionModel,
                                            ConfigurationModel configurationModel,
                                            MuleContext muleContext,
                                            Registry registry) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
    this.registry = registry;
  }

  @Override
  public ConfigurationProvider doGetObject() throws Exception {
    Map<String, String> rawParams = new HashMap<>();
    final Set<Entry<String, Object>> entrySet = getParameters().entrySet();
    for (Entry<String, Object> entry : entrySet) {
      rawParams.put(entry.getKey(), entry.getValue().toString());
    }

    return new XmlSdkConfigurationProvider(configName.get(), rawParams, extensionModel, configurationModel, registry);
  }

  public String getName() {
    return configurationModel.getAllParameterModels().stream()
        .filter(ParameterModel::isComponentId)
        .findAny()
        .map(p -> parameters.get(p.getName()).toString())
        .orElseThrow(() -> new RequiredParameterNotSetException("cannot create a configuration without a name"));
  }

}

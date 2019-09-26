/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.exception.RequiredParameterNotSetException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class XmlSdkConfigurationProviderFactory extends AbstractExtensionObjectFactory<ConfigurationProvider>
    implements ObjectFactory<ConfigurationProvider> {

  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;

  private final LazyValue<String> configName = new LazyValue<>(this::getName);
  private List<ConfigurationProvider> innerConfigProviders = emptyList();

  public XmlSdkConfigurationProviderFactory(ExtensionModel extensionModel,
                                            ConfigurationModel configurationModel,
                                            MuleContext muleContext) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
  }

  @Override
  public ConfigurationProvider doGetObject() throws Exception {
    Map<String, String> rawParams = new HashMap<>();
    final Set<Entry<String, Object>> entrySet = getParameters().entrySet();
    for (Entry<String, Object> entry : entrySet) {
      rawParams.put(entry.getKey(), entry.getValue().toString());
    }

    return new XmlSdkConfigurationProvider(configName.get(), innerConfigProviders, rawParams, extensionModel, configurationModel);
  }

  public List<ConfigurationProvider> getInnerConfigProviders() {
    return innerConfigProviders;
  }

  public void setInnerConfigProviders(List<ConfigurationProvider> innerConfigProviders) {
    this.innerConfigProviders = unmodifiableList(innerConfigProviders);
  }

  public String getName() {
    return configurationModel.getAllParameterModels().stream()
        .filter(ParameterModel::isComponentId)
        .findAny()
        .map(p -> parameters.get(p.getName()).toString())
        .orElseThrow(() -> new RequiredParameterNotSetException("cannot create a configuration without a name"));
  }

}

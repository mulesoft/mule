/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.exception.RequiredParameterNotSetException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

class XmlSdkConfigurationProviderFactory extends AbstractExtensionObjectFactory<ConfigurationProvider>
    implements ObjectFactory<ConfigurationProvider> {

  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  private final Registry registry;

  private final LazyValue<String> configName = new LazyValue<>(this::getName);
  private List<ConfigurationProvider> innerConfigProviders = emptyList();

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

    return new XmlSdkConfigurationProvider(configName.get(), innerConfigProviders, rawParams, extensionModel, configurationModel);
  }

  public List<ConfigurationProvider> getInnerConfigProviders() {
    return innerConfigProviders;
  }

  public void setInnerConfigProviders(List<ConfigurationProvider> innerConfigProviders) {
    //TODO: REMOVE THIS (MULE-17419)
    //This is needed to reference actual configuration instances injected in the Registry instead of
    //getting a new inner bean instance injected by Spring in this method
    this.innerConfigProviders = unmodifiableList(
            innerConfigProviders
                    .stream()
                    .map(icp ->
                                 new LazyGlobalConfigurationProvider(
                                         new LazyValue<>(
                                                 () -> this.registry.<ConfigurationProvider>lookupByName(icp.getName())
                                                         .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Attempting to reference ConfigurationProvider that was not yet created")))
                                         )
                                 )
                    ).collect(toList())
    );
  }

  public String getName() {
    return configurationModel.getAllParameterModels().stream()
        .filter(ParameterModel::isComponentId)
        .findAny()
        .map(p -> parameters.get(p.getName()).toString())
        .orElseThrow(() -> new RequiredParameterNotSetException("cannot create a configuration without a name"));
  }

  //TODO: REMOVE THIS (MULE-17419)
  private class LazyGlobalConfigurationProvider implements ConfigurationProvider {

    private LazyValue<ConfigurationProvider> lazyProvider;

    private LazyGlobalConfigurationProvider(LazyValue<ConfigurationProvider> lazyProvider) {
      this.lazyProvider = lazyProvider;
    }

    @Override
    public ConfigurationInstance get(Event event) {
      return lazyProvider.get().get(event);
    }

    @Override
    public ExtensionModel getExtensionModel() {
      return lazyProvider.get().getExtensionModel();
    }

    @Override
    public ConfigurationModel getConfigurationModel() {
      return lazyProvider.get().getConfigurationModel();
    }

    @Override
    public String getName() {
      return lazyProvider.get().getName();
    }

    @Override
    public boolean isDynamic() {
      return lazyProvider.get().isDynamic();
    }

    @Override
    public Object getAnnotation(QName name) {
      return lazyProvider.get().getAnnotation(name);
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return lazyProvider.get().getAnnotations();
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {
      lazyProvider.get().setAnnotations(annotations);
    }

    @Override
    public ComponentLocation getLocation() {
      return lazyProvider.get().getLocation();
    }

    @Override
    public Location getRootContainerLocation() {
      return lazyProvider.get().getRootContainerLocation();
    }
  }

}

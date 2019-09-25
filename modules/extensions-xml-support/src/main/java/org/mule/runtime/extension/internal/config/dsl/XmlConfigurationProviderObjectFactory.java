/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

public class XmlConfigurationProviderObjectFactory extends AbstractExtensionObjectFactory<ConfigurationProvider>
    implements ObjectFactory<ConfigurationProvider> {

  private final ExtensionModel extensionModel;
  private final ConfigurationModel configurationModel;
  // private final ConfigurationProviderFactory configurationProviderFactory = new DefaultConfigurationProviderFactory();

  private Map<String, Object> properties = emptyMap();

  private List innerConfigs = new ArrayList<>();

  public XmlConfigurationProviderObjectFactory(ExtensionModel extensionModel,
                                               ConfigurationModel configurationModel,
                                               MuleContext muleContext) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.configurationModel = configurationModel;
  }

  @Override
  public ConfigurationProvider doGetObject() throws Exception {
    // TODO Auto-generated method stub
    return new ConfigurationProvider() {

      @Override
      public void setAnnotations(Map<QName, Object> annotations) {
        // TODO Auto-generated method stub

      }

      @Override
      public Location getRootContainerLocation() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ComponentLocation getLocation() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Map<QName, Object> getAnnotations() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Object getAnnotation(QName name) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isDynamic() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public String getName() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ExtensionModel getExtensionModel() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ConfigurationModel getConfigurationModel() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ConfigurationInstance get(Event event) {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }


  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    System.out.println(" >> properties: " + properties.toString());
    this.properties = properties;
  }


  public List getInnerConfigs() {
    return innerConfigs;
  }

  public void setInnerConfigs(List innerConfigs) {
    System.out.println(" >> innerConfigs: " + innerConfigs.toString());
    this.innerConfigs = innerConfigs;
  }
  // private String getName() {
  // return configurationModel.getAllParameterModels().stream()
  // .filter(ParameterModel::isComponentId)
  // .findAny()
  // .map(p -> ((ValueResolver) parameters.get(p.getName())))
  // .map(vr -> {
  // try {
  // return ((String) vr.resolve(null));
  // } catch (MuleException e) {
  // throw new IllegalStateException("Error obtaining configuration name", e);
  // }
  // })
  // .orElseThrow(() -> new RequiredParameterNotSetException("cannot create a configuration without a name"));
  // }


}

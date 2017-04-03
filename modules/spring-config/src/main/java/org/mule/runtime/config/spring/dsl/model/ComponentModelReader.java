/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_DOMAIN_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.POLICY_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.VALUE_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.to;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.api.artifact.ArtifactProperties;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.SimpleConfigAttribute;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Class used to read xml files from {@link ConfigLine}s, unifying knowledge on how to properly read the files returning the
 * {@link ComponentModel} object.
 *
 * It also replaces the values of the attributes by using the {@link Properties} object parametrized in its constructor.
 */
public class ComponentModelReader {

  private PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
  private Properties applicationProperties;

  public ComponentModelReader(ArtifactProperties artifactProperties) {
    this.applicationProperties = new Properties();
    this.applicationProperties.putAll(artifactProperties.toImmutableMap());
  }

  public ComponentModel extractComponentDefinitionModel(ConfigLine configLine, String configFileName) {

    String namespace = configLine.getNamespace() == null ? CORE_PREFIX : configLine.getNamespace();
    ComponentModel.Builder builder = new ComponentModel.Builder()
        .setIdentifier(builder()
            .withNamespace(namespace)
            .withName(configLine.getIdentifier())
            .build())
        .setTextContent(configLine.getTextContent())
        .setConfigFileName(configFileName)
        .setLineNumber(configLine.getLineNumber());
    to(builder).addNode(from(configLine).getNode());
    for (SimpleConfigAttribute simpleConfigAttribute : configLine.getConfigAttributes().values()) {
      builder.addParameter(simpleConfigAttribute.getName(), resolveValueIfIsPlaceHolder(simpleConfigAttribute.getValue()),
                           simpleConfigAttribute.isValueFromSchema());
    }

    List<ComponentModel> componentModels = configLine.getChildren().stream()
        .map(childConfigLine -> extractComponentDefinitionModel(childConfigLine, configFileName))
        .collect(Collectors.toList());
    componentModels.stream().forEach(componentDefinitionModel -> {
      if (SPRING_PROPERTY_IDENTIFIER.equals(componentDefinitionModel.getIdentifier())) {
        String value = componentDefinitionModel.getParameters().get(VALUE_ATTRIBUTE);
        if (value != null) {
          builder.addParameter(componentDefinitionModel.getNameAttribute(), resolveValueIfIsPlaceHolder(value), false);
        }
      }
      builder.addChildComponentModel(componentDefinitionModel);
    });
    ConfigLine parent = configLine.getParent();
    if (parent != null && isConfigurationTopComponent(parent)) {
      builder.markAsRootComponent();
    }
    ComponentModel componentModel = builder.build();
    for (ComponentModel innerComponentModel : componentModel.getInnerComponents()) {
      innerComponentModel.setParent(componentModel);
    }
    return componentModel;
  }

  private String resolveValueIfIsPlaceHolder(String value) {
    return propertyPlaceholderHelper.replacePlaceholders(value, applicationProperties);
  }

  private boolean isConfigurationTopComponent(ConfigLine parent) {
    return (parent.getIdentifier().equals(MULE_ROOT_ELEMENT) || parent.getIdentifier().equals(MULE_DOMAIN_ROOT_ELEMENT) ||
        parent.getIdentifier().equals(POLICY_ROOT_ELEMENT));
  }
}

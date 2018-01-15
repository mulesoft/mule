/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_DOMAIN_ROOT_ELEMENT;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_ROOT_ELEMENT;
import static org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler.to;
import static org.mule.runtime.config.internal.model.ApplicationModel.POLICY_ROOT_ELEMENT;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.api.dsl.processor.SimpleConfigAttribute;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.ComponentModel;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Class used to read xml files from {@link ConfigLine}s, unifying knowledge on how to properly read the files returning the
 * {@link ComponentModel} object.
 *
 * It also replaces the values of the attributes by using the {@link Properties} object parametrized in its constructor.
 */
public class ComponentModelReader {

  private final ConfigurationPropertiesResolver configurationPropertiesResolver;

  public ComponentModelReader(ConfigurationPropertiesResolver configurationPropertiesResolver) {
    this.configurationPropertiesResolver = configurationPropertiesResolver;
  }

  public ComponentModel extractComponentDefinitionModel(ConfigLine configLine, String configFileName) {

    String namespace = configLine.getNamespace() == null ? CORE_PREFIX : configLine.getNamespace();
    ComponentModel.Builder builder = new ComponentModel.Builder()
        .setIdentifier(builder()
            .namespace(namespace)
            .name(configLine.getIdentifier())
            .build())
        .setTextContent(resolveValueIfIsPlaceHolder(configLine.getTextContent()))
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
    componentModels.stream().forEach(componentDefinitionModel -> builder.addChildComponentModel(componentDefinitionModel));
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
    Object resolvedValue = configurationPropertiesResolver.resolveValue(value);
    return resolvedValue instanceof String ? (String) resolvedValue : (resolvedValue != null ? resolvedValue.toString() : null);
  }

  private boolean isConfigurationTopComponent(ConfigLine parent) {
    return (parent.getIdentifier().equals(MULE_ROOT_ELEMENT) || parent.getIdentifier().equals(MULE_DOMAIN_ROOT_ELEMENT) ||
        parent.getIdentifier().equals(POLICY_ROOT_ELEMENT));
  }
}

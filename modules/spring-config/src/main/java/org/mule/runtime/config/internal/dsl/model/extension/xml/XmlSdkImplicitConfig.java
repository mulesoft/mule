/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.ComponentGenerationInformation.EMPTY_GENERATION_INFO;
import static org.mule.runtime.ast.api.ComponentMetadataAst.EMPTY_METADATA;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAst;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AST component that represent a configuration intended to be used when there is not a configuration on the application but there
 * are modules from the XML Sdk that have XML Sdk properties with default values.
 */
public class XmlSdkImplicitConfig extends BaseComponentAst {

  public static final String IMPLICIT_CONFIG_NAME_SUFFIX = "%s-xml-sdk-implicit-config";

  private final ExtensionModel extensionModel;
  private final String configName;
  private final Set<ComponentParameterAst> componentParameterAsts = new HashSet<>();

  public XmlSdkImplicitConfig(ExtensionModel extensionModel) {
    this.extensionModel = extensionModel;
    this.configName = format(IMPLICIT_CONFIG_NAME_SUFFIX, extensionModel.getName());

    final List<ParameterModel> parameterModels = getModel(ParameterizedModel.class)
        .map(ParameterizedModel::getParameterGroupModels)
        .orElse(emptyList())
        .stream()
        .filter(parameterGroupModel -> parameterGroupModel.getName().equals(DEFAULT_GROUP_NAME))
        .findFirst()
        .map(ParameterGroupModel::getParameterModels)
        .orElse(emptyList());

    for (ParameterModel parameterModel : parameterModels) {
      Object value = parameterModel.getName().equals("name") ? configName : parameterModel.getDefaultValue();
      componentParameterAsts.add(new XmlSdkImplicitConfigParameter(parameterModel, value));
    }
  }

  @Override
  public ComponentIdentifier getIdentifier() {
    return ComponentIdentifier.builder()
        .namespaceUri(extensionModel.getXmlDslModel().getNamespace())
        .namespace(extensionModel.getXmlDslModel().getPrefix())
        .name(configName).build();
  }

  @Override
  public ComponentType getComponentType() {
    return null;
  }

  @Override
  public ComponentLocation getLocation() {
    return from(configName);
  }

  @Override
  public ComponentMetadataAst getMetadata() {
    return EMPTY_METADATA;
  }

  @Override
  public ComponentGenerationInformation getGenerationInformation() {
    return EMPTY_GENERATION_INFO;
  }

  @Override
  public Optional<String> getComponentId() {
    return of(configName);
  }

  @Override
  public Map<String, Object> getAnnotations() {
    return emptyMap();
  }

  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  @Override
  public <M> Optional<M> getModel(Class<M> modelClass) {
    return (Optional<M>) extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)
        .filter(modelClass::isInstance);
  }

  @Override
  public MetadataType getType() {
    return null;
  }

  @Override
  public Collection<ComponentParameterAst> getParameters() {
    return componentParameterAsts;
  }

  @Override
  public List<ComponentAst> directChildren() {
    return emptyList();
  }

}

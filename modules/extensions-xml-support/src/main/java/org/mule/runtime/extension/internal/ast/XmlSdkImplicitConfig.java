/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.ast;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONFIG;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.ast.api.ComponentGenerationInformation.EMPTY_GENERATION_INFO;
import static org.mule.runtime.ast.api.ComponentMetadataAst.EMPTY_METADATA;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.runtime.extension.internal.ast.MacroExpansionModuleModel.MODULE_CONFIG_GLOBAL_ELEMENT_NAME;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.ParameterResolutionException;
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
        .map(ParameterizedModel::getAllParameterModels).orElse(emptyList());

    for (ParameterModel parameterModel : parameterModels) {
      String rawValue = parameterModel.getName().equals("name") ? configName : null;
      ComponentParameterAst componentParameterAst = getComponentParameterAst(parameterModel, rawValue);
      componentParameterAsts.add(componentParameterAst);
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
    return CONFIG;
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

  private ComponentParameterAst getComponentParameterAst(ParameterModel parameterModel, String rawValue) {
    return new ComponentParameterAst() {

      @Override
      public ParameterModel getModel() {
        return parameterModel;
      }

      @Override
      public ParameterGroupModel getGroupModel() {
        return null;
      }

      @Override
      public Either<String, Object> getValue() {
        return right(getRawValue() != null ? getRawValue() : getModel().getDefaultValue());
      }

      @Override
      public <T> Either<String, Either<ParameterResolutionException, T>> getValueOrResolutionError() {
        return null;
      }

      @Override
      public String getRawValue() {
        return rawValue;
      }

      @Override
      public String getResolvedRawValue() {
        return null;
      }

      @Override
      public Optional<ComponentMetadataAst> getMetadata() {
        return empty();
      }

      @Override
      public ComponentGenerationInformation getGenerationInformation() {
        return EMPTY_GENERATION_INFO;
      }

      @Override
      public boolean isDefaultValue() {
        return true;
      }
    };

  }

}

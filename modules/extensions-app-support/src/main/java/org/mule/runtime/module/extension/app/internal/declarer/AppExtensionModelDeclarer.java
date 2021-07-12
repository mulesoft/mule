/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.app.internal.declarer;

import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.type.ApplicationTypeLoader;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.model.parameter.ImmutableExclusiveParametersModel;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ExclusiveOptionalModelProperty;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppExtensionModelDeclarer {

  private ApplicationTypeLoader typeLoader;

  public void declare(ArtifactDescriptor artifactDescriptor, ArtifactType artifactType, ArtifactAst ast) {
    ExtensionDeclarer declarer = new ExtensionDeclarer();
    final String appName = artifactDescriptor.getName();
    declarer.named(appName)
            .describedAs(appName + " " + artifactType.getAsString())
            .onVersion(artifactDescriptor.getBundleDescriptor().getVersion());

    ast.topLevelComponentsStream()
            .filter(c -> c.getComponentType() == OPERATION_DEF)
            .forEach(c -> declareOperation(declarer, c));
  }

  private void declareOperation(HasOperationDeclarer declarer, ComponentAst operationDefAst) {
    OperationDeclarer operation = declarer.withOperation(requiredString(operationDefAst, "name"))
            .describedAs(optionalString(operationDefAst, "description"));

    parseComponentDisplayModel(operation.getDeclaration(), operationDefAst);
    parseOperationOutput(operation, operationDefAst);
    parseParameters(operation, operationDefAst);
  }

  private void parseParameters(ParameterizedDeclarer declarer, ComponentAst componentAst) {
    ComponentParameterAst parametersElement = componentAst.getParameter("parameters");
    if (parametersElement == null) {
      return;
    }

    List<ComponentAst> parameters = (List<ComponentAst>) parametersElement.getValue().getRight();
    if (parameters != null) {
      parameters.forEach(p -> declareParameter(declarer, p));
    }
  }

  private void declareParameter(ParameterizedDeclarer declarer, ComponentAst parameterAst) {
    final String paramName = requiredString(parameterAst, "name");
    ParameterDeclarer parameter;

    ComponentParameterAst optionalElement = parameterAst.getParameter("optional");

    if (optionalElement != null) {
      ComponentAst optional = (ComponentAst) optionalElement.getValue().getRight();
      parameter = declarer.onDefaultParameterGroup().withOptionalParameter(paramName)
              .defaultingTo(optionalString(optional, "defaultValue"));

      ComponentParameterAst exclusiveOptional = optional.getParameter("exclusiveOptional");
      if (exclusiveOptional != null) {
        parameter.withModelProperty(new ExclusiveOptionalModelProperty(parseExclusiveParametersModel((ComponentAst) exclusiveOptional.getValue().getRight())));
      }
    } else {
      parameter = declarer.onDefaultParameterGroup().withRequiredParameter(paramName);
    }

    parameter.describedAs(optionalString(parameterAst, "description"))
            .ofType(typeLoader.load())
            .withExpressionSupport()
            .withRole(BEHAVIOUR);
  }

  private void parseOperationOutput(OperationDeclarer operation, ComponentAst operationDefAst) {
    ComponentAst output = operationDefAst.directChildrenStreamByIdentifier(null, "output")
            .findFirst()
            .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
                    "Operation '%s' is missing its <output> declaration", operation.getDeclaration().getName())));

    ComponentAst payloadType = output.directChildrenStreamByIdentifier(null, "payload-type")
            .findFirst()
            .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
                    "Operation '%s' is missing its <payload-type> declaration", operation.getDeclaration().getName())));

    parseOutputResultType(operation.withOutput(), operationDefAst, payloadType);
    output.directChildrenStreamByIdentifier(null, "payload-type")
            .findFirst()
            .ifPresent(attrType -> parseOutputResultType(operation.withOutputAttributes(), operationDefAst, attrType));
  }

  private void parseComponentDisplayModel(BaseDeclaration declaration, ComponentAst componentAst) {
    String summary = optionalString(componentAst, "summary");
    String displayName = optionalString(componentAst, "displayName");

    if (!isBlank(displayName) || !isBlank(summary)) {
      declaration.setDisplayModel(DisplayModel.builder()
              .summary(summary)
              .displayName(displayName).build());
    }
  }

  private void parseOutputResultType(OutputDeclarer declarer,
                                     ComponentAst componentAst,
                                     ComponentAst resultDefElement) {
    String type = requiredString(resultDefElement, "type");
    String mimeType = optionalString(resultDefElement, "mimeType", "application/java");
    declarer.ofType(typeLoader.load(type, mimeType).orElseThrow(() -> new IllegalModelDefinitionException(format(
            "Component <%s:%s> defines %s as '%s' with mediaType '%s', but such type is not defined in the application",
            componentAst.getIdentifier().getNamespace(),
            componentAst.getIdentifier().getName(),
            resultDefElement.getIdentifier().getName(),
            mimeType))));
  }

  private String requiredString(ComponentAst ast, String paramName) {
    String value = getResolvedParameter(ast, paramName);

    if (isBlank(value)) {
      throw new IllegalModelDefinitionException(format("Element <%s:s> defines a blank value for required parameter '%s'. Component Location: %s",
              ast.getIdentifier().getNamespace(), ast.getIdentifier().getName(), ast.getLocation().getLocation()));
    }
    return value;
  }

  private String optionalString(ComponentAst ast, String paramName) {
    return optionalString(ast, paramName, null);
  }

  private String optionalString(ComponentAst ast, String paramName, String defaultValue) {
    String value = getResolvedParameter(ast, paramName);
    return value != null ? value : defaultValue;
  }

  private String getResolvedParameter(ComponentAst ast, String paramName) {
    ComponentParameterAst parameter = ast.getParameter(paramName);
    return parameter != null ? parameter.getResolvedRawValue() : null;
  }

  private ExclusiveParametersModel parseExclusiveParametersModel(ComponentAst componentAst) {
    Set<String> parameters = Stream.of(requiredString(componentAst, "parameters").split(","))
            .map(String::trim)
            .filter(p -> !isBlank(p))
            .collect(toCollection(LinkedHashSet::new));

    return new ImmutableExclusiveParametersModel(parameters, (Boolean) componentAst.getParameter("oneRequired").getValue().getRight());
  }
}

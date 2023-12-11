/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.ast;

import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyComponentTreeRecursively;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.internal.loader.xml.TlsEnabledComponentUtils.isTlsContextFactoryParameter;
import static org.mule.runtime.extension.internal.loader.xml.TlsEnabledComponentUtils.isTlsEnabled;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.ast.api.util.BaseComponentParameterAstDecorator;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Decorates a {@link ComponentAst} that represents an element defined in an Xml Sdk 1 connector so that its macro-expansion
 * occurs when it is being processed.
 *
 * @since 4.4
 */
class MacroExpandedComponentAst extends BaseComponentAstDecorator {

  private static final String TLS_CONTEXT_CONFIG_PARAMETER_KEY = getTlsContextConfigParameterKey();

  private static String getTlsContextConfigParameterKey() {
    return "#[vars." + InfrastructureTypeMapping.getMap().get(TlsContextFactory.class).getName() + "]";
  }

  private final ComponentLocation location;
  private final Set<String> moduleGlobalElementsNames;
  private final String defaultGlobalElementSuffix;
  private final Map<String, Object> literalsParameters;
  private final List<ComponentAst> macroExpandedChildren;
  private final boolean isTlsEnabled;
  private final Map<String, ComponentParameterAst> mappedParameters = new ConcurrentHashMap<>();

  public MacroExpandedComponentAst(ComponentAst original, ComponentLocation location,
                                   Set<String> moduleGlobalElementsNames, String defaultGlobalElementSuffix,
                                   List<ComponentAst> macroExpandedChildren) {
    this(original, location, moduleGlobalElementsNames, defaultGlobalElementSuffix, emptyMap(), macroExpandedChildren);
  }

  public MacroExpandedComponentAst(ComponentAst original, ComponentLocation location,
                                   Set<String> moduleGlobalElementsNames, String defaultGlobalElementSuffix,
                                   Map<String, Object> literalsParameters,
                                   List<ComponentAst> macroExpandedChildren) {
    super(original);
    this.location = location;
    this.moduleGlobalElementsNames = moduleGlobalElementsNames;
    this.defaultGlobalElementSuffix = defaultGlobalElementSuffix;
    this.literalsParameters = literalsParameters;
    this.macroExpandedChildren = macroExpandedChildren;
    this.isTlsEnabled = isTlsEnabled(original);
  }

  @Override
  public Stream<ComponentAst> recursiveStream() {
    return concat(concat(Stream.of(this),
                         getDecorated().directChildrenStream().flatMap(ComponentAst::recursiveStream)),
                  directChildrenStream()
                      .flatMap(ComponentAst::recursiveStream));
  }

  @Override
  public Stream<ComponentAst> directChildrenStream() {
    return macroExpandedChildren.stream();
  }

  @Override
  public ComponentParameterAst getParameter(String parameterGroup, String parameterName) {
    final ComponentParameterAst parameter = super.getParameter(parameterGroup, parameterName);
    return parameter != null ? resolveParameter(parameter) : null;
  }

  private ComponentParameterAst resolveParameter(ComponentParameterAst parameter) {
    String resolvedParameterKey = parameter.getGroupModel() == null ? parameter.getModel().getName()
        : parameter.getGroupModel().getName() + "-" + parameter.getModel().getName();
    mappedParameters.putIfAbsent(resolvedParameterKey, mapIdParam(parameter));
    return mappedParameters.get(resolvedParameterKey);
  }

  @Override
  public Collection<ComponentParameterAst> getParameters() {
    return super.getParameters()
        .stream()
        .map(this::resolveParameter)
        .collect(toList());
  }

  private boolean mustExpandTlsContextParameter(ComponentParameterAst parameter) {
    return isTlsEnabled && isTlsContextFactoryParameter(parameter);
  }

  private ComponentParameterAst mapIdParam(final ComponentParameterAst originalParameter) {
    requireNonNull(originalParameter);
    return new BaseComponentParameterAstDecorator(originalParameter) {

      private final LazyValue<Either<String, ?>> expandedParameter = new LazyValue<>(this::expandParameter);

      @Override
      public <T> Either<String, T> getValue() {
        return (Either<String, T>) expandedParameter.get();
      }

      private <T> Either<String, T> expandParameter() {

        // TLS context parameter is expanded with custom logic.
        if (mustExpandTlsContextParameter(getDecorated()) && literalsParameters.containsKey(TLS_CONTEXT_CONFIG_PARAMETER_KEY)) {
          return right((T) literalsParameters.get(TLS_CONTEXT_CONFIG_PARAMETER_KEY));
        }

        // The rest of the simple parameters are expanded if necessary.
        final Either<String, T> originalValue = getDecorated().getValue();
        if (originalValue.isLeft()) {
          final String expression = "#[" + originalValue.getLeft() + "]";
          if (literalsParameters.containsKey(expression)) {
            return right((T) literalsParameters.get(expression));
          } else {
            return originalValue;
          }
        }

        // Complex parameters are expanded by this call.
        return (Either<String, T>) originalValue
            .mapRight(this::mapParameter);
      }

      private <T> Object mapParameter(T rawValue) {
        if (mustMacroExpandRawValue(rawValue)) {
          return macroExpandedRawValue((String) rawValue);
        } else if (isAnErrorMappings()) {
          return mapErrorMappings(getDecorated());
        } else if (supportsChildDeclaration() && !isResolvedByMetadataVisitor(rawValue)) {
          return macroExpandComplexParameter();
        }
        return rawValue;
      }

      private <T> boolean mustMacroExpandRawValue(T rawValue) {
        // Evaluates if the parameter is not declared as a child and can be an expression so that it is resolved.
        // In that case, the raw value is always a String.
        return rawValue instanceof String;
      }

      private boolean isAnErrorMappings() {
        return getDecorated().getModel().getName().equals(ERROR_MAPPINGS_PARAMETER_NAME);
      }

      private <T> boolean isResolvedByMetadataVisitor(T rawValue) {
        // These cases are resolved by a MetadataTypeVisitor in MuleAstUtils.
        // See the visitArrayType and visitObject methods.
        return rawValue instanceof List || rawValue instanceof Map;
      }

      private boolean supportsChildDeclaration() {
        Optional<DslElementSyntax> dslElementSyntax = getDecorated().getGenerationInformation().getSyntax();
        return dslElementSyntax.map(DslElementSyntax::supportsChildDeclaration).orElse(false);
      }

      private Object mapErrorMappings(final ComponentParameterAst originalParameter) {
        return originalParameter.getValue()
            .mapRight(mappings -> ((List<ErrorMapping>) mappings)
                .stream()
                .map(mapping -> new ErrorMapping(literalsParameters.getOrDefault(mapping.getSource(), mapping.getSource())
                    .toString(),
                                                 literalsParameters.getOrDefault(mapping.getTarget(),
                                                                                 mapping.getTarget())
                                                     .toString()))
                .collect(toList()))
            .getRight();
      }

      private Object macroExpandComplexParameter() {
        ComponentAst component = (ComponentAst) getDecorated().getValue().getRight();
        MacroExpandedComponentAst macroExpandedComponentAst =
            new MacroExpandedComponentAst(component, component.getLocation(), moduleGlobalElementsNames,
                                          defaultGlobalElementSuffix, literalsParameters, macroExpandedChildren);
        return copyComponentTreeRecursively(macroExpandedComponentAst, identity());
      }

      @Override
      public String getRawValue() {
        return macroExpandedRawValue(getDecorated().getRawValue());
      }

      @Override
      public String getResolvedRawValue() {
        return macroExpandedRawValue(getDecorated().getResolvedRawValue());
      }

      private String macroExpandedRawValue(final String originalRawValue) {
        if (originalRawValue != null) {
          if (moduleGlobalElementsNames.contains(originalRawValue)) {
            return originalRawValue.concat("-").concat(defaultGlobalElementSuffix);
          } else {
            return literalsParameters.getOrDefault(originalRawValue, originalRawValue).toString();
          }
        } else {
          return null;
        }
      }

    };
  }

  @Override
  public Optional<String> getComponentId() {
    return super.getComponentId()
        .map(this::mapParamValue);
  }

  private String mapParamValue(String originalValue) {
    if (moduleGlobalElementsNames.contains(originalValue)) {
      return originalValue.concat("-").concat(defaultGlobalElementSuffix);
    } else {
      // not a global element, returning the original value.
      return literalsParameters.getOrDefault(originalValue, originalValue).toString();
    }
  }

  @Override
  public ComponentLocation getLocation() {
    return location;
  }

  @Override
  public String toString() {
    return super.toString() + " - macroExpanded";
  }
}

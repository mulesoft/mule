/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.ast;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.ast.api.util.BaseComponentParameterAstDecorator;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Decorates a {@link ComponentAst} that represents an element defined in an Xml Sdk 1 connector so that its macro-expansion
 * occurs when it is being processed.
 *
 * @since 4.4
 */
class MacroExpandedComponentAst extends BaseComponentAstDecorator {

  private final ComponentLocation location;
  private final Set<String> moduleGlobalElementsNames;
  private final String defaultGlobalElementSuffix;
  private final Map<String, String> literalsParameters;

  private final List<ComponentAst> macroExpandedChildren;

  public MacroExpandedComponentAst(ComponentAst original, ComponentLocation location,
                                   Set<String> moduleGlobalElementsNames, String defaultGlobalElementSuffix,
                                   List<ComponentAst> macroExpandedChildren) {
    this(original, location, moduleGlobalElementsNames, defaultGlobalElementSuffix, emptyMap(), macroExpandedChildren);
  }

  public MacroExpandedComponentAst(ComponentAst original, ComponentLocation location,
                                   Set<String> moduleGlobalElementsNames, String defaultGlobalElementSuffix,
                                   Map<String, String> literalsParameters,
                                   List<ComponentAst> macroExpandedChildren) {
    super(original);
    this.location = location;
    this.moduleGlobalElementsNames = moduleGlobalElementsNames;
    this.defaultGlobalElementSuffix = defaultGlobalElementSuffix;
    this.literalsParameters = literalsParameters;
    this.macroExpandedChildren = macroExpandedChildren;
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
  public ComponentParameterAst getParameter(String groupName, String paramName) {
    final ComponentParameterAst parameter = super.getParameter(groupName, paramName);
    return parameter != null ? mapIdParam(parameter) : null;
  }

  @Override
  public Collection<ComponentParameterAst> getParameters() {
    return super.getParameters()
        .stream()
        .map(this::mapIdParam)
        .collect(toList());
  }

  private ComponentParameterAst mapIdParam(final ComponentParameterAst originalParameter) {
    requireNonNull(originalParameter);
    return new BaseComponentParameterAstDecorator(originalParameter) {

      @Override
      public <T> Either<String, T> getValue() {
        final Either<String, T> originalValue = getDecorated().getValue();

        if (originalValue.isLeft()) {
          final String expression = "#[" + originalValue.getLeft() + "]";
          if (literalsParameters.containsKey(expression)) {
            // Do the Gorvachev
            return right((T) literalsParameters.get(expression));
          } else {
            return originalValue;
          }
        }

        return (Either<String, T>) originalValue
            .mapRight(v -> {
              if (v instanceof String) {
                return macroExpandedRawValue((String) v);
              } else if (getDecorated().getModel().getName().equals(ERROR_MAPPINGS_PARAMETER_NAME)) {
                return mapErrorMappings(getDecorated());
              }
              return v;
            });
      }

      private Object mapErrorMappings(final ComponentParameterAst originalParameter) {
        return originalParameter.getValue()
            .mapRight(mappings -> ((List<ErrorMapping>) mappings)
                .stream()
                .map(mapping -> new ErrorMapping(literalsParameters.getOrDefault(mapping.getSource(), mapping.getSource()),
                                                 literalsParameters.getOrDefault(mapping.getTarget(),
                                                                                 mapping.getTarget())))
                .collect(toList()))
            .getRight();
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
            return literalsParameters.getOrDefault(originalRawValue, originalRawValue);
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
      return literalsParameters.getOrDefault(originalValue, originalValue);
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

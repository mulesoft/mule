/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;

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
  public ComponentParameterAst getParameter(String paramName) {
    return mapIdParam(super.getParameter(paramName));
  }

  @Override
  public Collection<ComponentParameterAst> getParameters() {
    return super.getParameters()
        .stream()
        .map(this::mapIdParam)
        .collect(toList());
  }

  private ComponentParameterAst mapIdParam(final ComponentParameterAst originalParameter) {
    return new ComponentParameterAst() {

      @Override
      public <T> Either<String, T> getValue() {
        return (Either<String, T>) originalParameter.getValue()
            .mapLeft(expr -> {
              return literalsParameters.getOrDefault(expr, expr);
            })
            .mapRight(v -> {
              if (v instanceof String) {
                String stringValue = (String) v;
                if (moduleGlobalElementsNames.contains(stringValue)) {
                  return stringValue.concat("-").concat(defaultGlobalElementSuffix);
                } else {
                  return literalsParameters.getOrDefault(stringValue, stringValue);
                }
              }
              return null;
            });
      }

      @Override
      public String getRawValue() {
        final String originalRawValue = originalParameter.getRawValue();
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

      @Override
      public ParameterModel getModel() {
        return originalParameter.getModel();
      }

      @Override
      public Optional<ComponentMetadataAst> getMetadata() {
        return originalParameter.getMetadata();
      }
    };
  }

  @Override
  public Optional<String> getRawParameterValue(String paramName) {
    return super.getRawParameterValue(paramName)
        .map(this::mapParamValue);
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

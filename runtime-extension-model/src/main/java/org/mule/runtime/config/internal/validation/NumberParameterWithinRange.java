/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.MuleAstUtils.hasPropertyPlaceholder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getGroupAndParametersPairs;

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.annotation.IntAnnotation;
import org.mule.metadata.api.annotation.NumberRangeAnnotation;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.ParameterResolutionException;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class NumberParameterWithinRange implements Validation {

  private final boolean ignoreParamsWithProperties;

  public NumberParameterWithinRange(boolean ignoreParamsWithProperties) {
    this.ignoreParamsWithProperties = ignoreParamsWithProperties;
  }

  @Override
  public String getName() {
    return "Number parameter within range.";
  }

  @Override
  public String getDescription() {
    return "Value of a Number type parameter is within the defined range.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> comp.getModel(ParameterizedModel.class)
        .map(pmzd -> getGroupAndParametersPairs(pmzd)
            .anyMatch(groupAndParameter -> isDoValidation(groupAndParameter.getSecond(),
                                                          comp.getParameter(groupAndParameter.getFirst().getName(),
                                                                            groupAndParameter.getSecond().getName()))))
        .orElse(false));
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return (component.getModel(ParameterizedModel.class)
        .map(pmzd -> getGroupAndParametersPairs(pmzd)
            .map(groupAndParameter -> new Pair<>(groupAndParameter.getSecond(),
                                                 component.getParameter(groupAndParameter.getFirst().getName(),
                                                                        groupAndParameter.getSecond().getName())))
            .filter(modelAndParam -> isDoValidation(modelAndParam.getFirst(), modelAndParam.getSecond())))
        .orElse(Stream.empty())
        .map(param -> {
          if (numberOffRange(param.getFirst(), param.getSecond())) {
            return of(create(component, param.getSecond(), this,
                             format("Parameter '%s' in element <%s> value '%s' is not within expected range.",
                                    param.getFirst().getName(),
                                    component.getIdentifier().toString(),
                                    param.getSecond().getResolvedRawValue())));
          } else if (param.getSecond().getValueOrResolutionError().isRight()
              && param.getSecond().getValueOrResolutionError().getRight().getRight() instanceof ComponentAst) {
            // validate any nested pojos as well...
            return validate((ComponentAst) param.getSecond().getValueOrResolutionError().getRight().getRight(), artifact);
          } else {
            return Optional.<ValidationResultItem>empty();
          }
        }))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private boolean numberOffRange(ParameterModel paramModel, ComponentParameterAst paramAst) {
    Optional<Either<ParameterResolutionException, Number>> value = paramAst.getValueOrResolutionError().getValue();

    if (paramAst == null || !value.isPresent()) {
      return false;
    }

    final NumberWithinRangeVisitor visitor = new NumberWithinRangeVisitor(value);
    paramModel.getType()
        .accept(visitor);

    return visitor.isValueOffRange();
  }

  protected boolean isDoValidation(ParameterModel pm, ComponentParameterAst componentParameterAst) {
    if (ignoreParamsWithProperties && hasPropertyPlaceholder(componentParameterAst.getRawValue())) {
      return false;
    }

    final NumberHasRangeVisitor visitor = new NumberHasRangeVisitor();
    pm.getType().accept(visitor);
    return visitor.hasRangeAnnotation();
  }

  private static final class NumberWithinRangeVisitor extends MetadataTypeVisitor {

    private final Optional<Either<ParameterResolutionException, Number>> value;
    private boolean valueOffRange = true;

    private NumberWithinRangeVisitor(Optional<Either<ParameterResolutionException, Number>> value) {
      this.value = value;
    }

    public boolean isValueOffRange() {
      return valueOffRange;
    }

    @Override
    public void visitNumber(NumberType numberType) {
      final NumberRangeAnnotation rangeAnnotation = numberType.getAnnotation(NumberRangeAnnotation.class).get();
      final Number valueAsNumber = value.get().getRight();

      if (numberType.getAnnotation(IntAnnotation.class).isPresent()) {
        rangeAnnotation.getFrom()
            .map(from -> valueAsNumber.longValue() < from.longValue())
            .ifPresent(v -> valueOffRange = v);

        rangeAnnotation.getTo()
            .map(to -> valueAsNumber.longValue() > to.longValue())
            .ifPresent(v -> valueOffRange = v);
      } else {
        rangeAnnotation.getFrom()
            .map(from -> valueAsNumber.doubleValue() < from.doubleValue())
            .ifPresent(v -> valueOffRange = v);

        rangeAnnotation.getTo()
            .map(to -> valueAsNumber.doubleValue() > to.doubleValue())
            .ifPresent(v -> valueOffRange = v);
      }

    }
  }

  private static final class NumberHasRangeVisitor extends MetadataTypeVisitor {

    private boolean rangeAnnotation = false;

    @Override
    public void visitNumber(NumberType numberType) {
      rangeAnnotation = numberType.getAnnotation(NumberRangeAnnotation.class).isPresent();
    }

    public boolean hasRangeAnnotation() {
      return rangeAnnotation;
    }
  }

}

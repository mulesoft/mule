/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.DefaultComponentParameterAst;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * Specialization of {@link ComponentModel} that keeps references to a Spring bean specification.
 *
 * @since 4.0
 */
public class SpringComponentModel extends ComponentModel implements ComponentAst {

  private final Map<String, ComponentParameterAst> parameterAsts = new HashMap<>();

  // TODO MULE-9688 Remove this attributes since should not be part of this class. This class should be immutable.
  private BeanReference beanReference;
  private BeanDefinition beanDefinition;

  /**
   * @param beanDefinition the {@code BeanDefinition} created based on the {@code ComponentModel} values.
   */
  public void setBeanDefinition(BeanDefinition beanDefinition) {
    this.beanDefinition = beanDefinition;
  }

  /**
   * @return the {@code BeanDefinition} created based on the {@code ComponentModel} values.
   */
  public BeanDefinition getBeanDefinition() {
    return beanDefinition;
  }

  /**
   * @param beanReference the {@code BeanReference} that represents this object.
   */
  public void setBeanReference(BeanReference beanReference) {
    this.beanReference = beanReference;
  }

  /**
   * @return the {@code BeanReference} that represents this object.
   */
  public BeanReference getBeanReference() {
    return beanReference;
  }

  @Override
  public ComponentLocation getLocation() {
    return getComponentLocation();
  }

  @Override
  public Optional<String> getComponentId() {
    return ofNullable(getParameters().get(NAME_ATTRIBUTE_NAME));
  }

  @Override
  public Optional<String> getRawParameterValue(String paramName) {
    if (paramName.equals(BODY_RAW_PARAM_NAME)) {
      return ofNullable(getTextContent());
    } else {
      return ofNullable(getParameters().get(paramName));
    }
  }

  /**
   * @param parameterName name of the configuration parameter.
   * @param value value contained by the configuration parameter.
   */
  @Override
  public void setParameter(ParameterModel parameterModel, ComponentParameterAst value) {
    super.setParameter(parameterModel, value);
    this.parameterAsts.put(parameterModel.getName(), value);
  }

  /**
   * @param paramName the name of the parameter to get AST for.
   * @return the AST of the parameter if present, or {@link Optional#empty()} if not present.
   */
  @Override
  public ComponentParameterAst getParameter(String paramName) {
    if (parameterAsts.containsKey(paramName)) {
      return parameterAsts.get(paramName);
    }

    Optional<ParameterModel> parameterModel = findParameterModel(paramName);
    parameterModel.orElseGet(() -> {
      validateParameter(paramName);
      return null;
    });

    return parameterModel.map(paramModel -> getRawParameterValue(paramName)
        .map(rawParamValue -> {
          return new DefaultComponentParameterAst(rawParamValue, () -> parameterModel.orElseGet(() -> {
            validateParameter(paramName);
            return null;
          }));
        })
        .orElseGet(() -> new DefaultComponentParameterAst(null, () -> parameterModel.orElseGet(() -> {
          validateParameter(paramName);
          return null;
        }))))
        .get();
  }

  private void validateParameter(String paramName) {
    if (!SpringComponentModel.this.getModel(ParameterizedModel.class).isPresent()) {
      throw new NoSuchElementException(" >>>> Wanted paramName '" + paramName + "'. The model is not parameterizable ("
          + SpringComponentModel.this.getModel(NamedObject.class) + ")");
    } else {
      throw new NoSuchElementException(" >>>> Wanted paramName '" + paramName + "'. Available: "
          + SpringComponentModel.this.getModel(ParameterizedModel.class).get().getAllParameterModels().stream()
              .map(pm -> pm.getName()).collect(toList()));
    }
  }

  private Optional<ParameterModel> findParameterModel(String paramName) {
    // For sources, we need to account for the case where parameters in the callbacks may have colliding names.
    // This logic ensures that the parameter fetching logic is consistent with the logic that handles this scenario in previous
    // implementations.
    Optional<ParameterModel> parameterModel = getModel(SourceModel.class)
        .flatMap(sourceModel -> {
          if (sourceModel.getErrorCallback().isPresent()) {
            final Optional<ParameterModel> findFirst = sourceModel.getErrorCallback().get().getAllParameterModels()
                .stream()
                .filter(pm -> pm.getName().equals(paramName))
                .findFirst();

            if (findFirst.isPresent()) {
              return findFirst;
            }
          }

          if (sourceModel.getSuccessCallback().isPresent()) {
            return sourceModel.getSuccessCallback().get().getAllParameterModels()
                .stream()
                .filter(pm -> pm.getName().equals(paramName))
                .findFirst();
          }

          return empty();
        });

    if (!parameterModel.isPresent()) {
      parameterModel = getModel(ParameterizedModel.class)
          .flatMap(parameterizedModel -> parameterizedModel.getAllParameterModels()
              .stream()
              .filter(pm -> pm.getName().equals(paramName))
              .findFirst());
    }

    return parameterModel;
  }

  @Override
  public Stream<ComponentAst> recursiveStream() {
    return StreamSupport.stream(recursiveSpliterator(), false);
  }

  @Override
  public Spliterator<ComponentAst> recursiveSpliterator() {
    return new Spliterator<ComponentAst>() {

      private boolean rootProcessed = false;

      private Spliterator<ComponentAst> currentChildSpliterator;

      private Spliterator<ComponentAst> innerSpliterator;

      @Override
      public boolean tryAdvance(Consumer<? super ComponentAst> action) {
        return doTryAdvance(action);
      }

      protected boolean doTryAdvance(Consumer<? super ComponentAst> action) {
        if (!rootProcessed) {
          rootProcessed = true;
          action.accept(SpringComponentModel.this);
          return true;
        }

        if (innerSpliterator == null) {
          innerSpliterator = directChildrenStream().map(ic -> ic).spliterator();
        }

        if (currentChildSpliterator != null) {
          if (currentChildSpliterator.tryAdvance(action)) {
            return true;
          } else {
            currentChildSpliterator = null;
            return doTryAdvance(action);
          }
        } else {
          if (innerSpliterator.tryAdvance(cm -> {
            currentChildSpliterator = cm.recursiveSpliterator();
          })) {
            return doTryAdvance(action);
          } else {
            return false;
          }
        }
      }

      @Override
      public Spliterator<ComponentAst> trySplit() {
        if (innerSpliterator == null) {
          innerSpliterator = directChildrenStream().map(ic -> ic).spliterator();
        }
        return null;
      }

      @Override
      public long estimateSize() {
        return 1 + directChildrenStream()
            .mapToLong(inner -> inner.recursiveSpliterator().estimateSize())
            .sum();
      }

      @Override
      public int characteristics() {
        return ORDERED | DISTINCT | SIZED | NONNULL | IMMUTABLE | SUBSIZED;
      }

    };
  }

  @Override
  public Stream<ComponentAst> directChildrenStream() {
    return getInnerComponents().stream().map(cm -> (ComponentAst) cm);
  }

  @Override
  public Spliterator<ComponentAst> directChildrenSpliterator() {
    return directChildrenStream().spliterator();
  }

  @Override
  public String toString() {
    return getComponentId().map(n -> "" + n + "(" + getIdentifier().toString() + ")").orElse(getIdentifier().toString())
        + (getLocation() != null ? (" @ " + getLocation().getLocation()) : "");
  }
}

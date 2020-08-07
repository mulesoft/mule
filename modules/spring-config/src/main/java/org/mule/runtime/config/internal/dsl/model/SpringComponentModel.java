/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.DefaultComponentParameterAst;
import org.mule.runtime.core.api.config.MuleConfiguration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
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
  private String componentId;
  private final AtomicBoolean parameterAstsPopulated = new AtomicBoolean(false);

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
    if (getType() != null && MuleConfiguration.class.isAssignableFrom(getType())) {
      return of(OBJECT_MULE_CONFIGURATION);
    } else if (getModel(ConstructModel.class)
        .map(cm -> cm.getName().equals("object"))
        .orElse(false)) {
      return ofNullable(getRawParameters().get(NAME_ATTRIBUTE_NAME));
    } else if (getIdentifier().equals(ERROR_HANDLER_IDENTIFIER) && getRawParameterValue("ref").isPresent()) {
      return empty();
    } else if (getModel(ParameterizedModel.class).isPresent()) {
      populateParameterAsts();
      return ofNullable(componentId);
    } else {
      // fallback for dsl elements that do not have an extension model declaration
      return ofNullable(getRawParameters().get(NAME_ATTRIBUTE_NAME));
    }
  }

  @Override
  public Optional<String> getRawParameterValue(String paramName) {
    if (paramName.equals(BODY_RAW_PARAM_NAME)) {
      return ofNullable(getTextContent());
    } else {
      return ofNullable(getRawParameters().get(paramName));
    }
  }

  /**
   * @param parameterName name of the configuration parameter.
   * @param value value contained by the configuration parameter.
   */
  @Override
  public void setParameter(ParameterModel parameterModel, ComponentParameterAst value) {
    parameterAstsPopulated.set(false);

    super.setParameter(parameterModel, value);
    this.parameterAsts.put(parameterModel.getName(), value);
  }

  /**
   * @param paramName the name of the parameter to get AST for.
   * @return the AST of the parameter if present, or {@link Optional#empty()} if not present.
   */
  @Override
  public ComponentParameterAst getParameter(String paramName) {
    populateParameterAsts();
    return parameterAsts.get(paramName);
  }

  @Override
  public Collection<ComponentParameterAst> getParameters() {
    populateParameterAsts();
    return parameterAsts.values()
        .stream()
        .filter(param -> param.getValue().getValue().isPresent())
        .collect(toSet());
  }

  private void populateParameterAsts() {
    if (!parameterAstsPopulated.compareAndSet(false, true)) {
      return;
    }

    if (!getModel(ParameterizedModel.class).isPresent()) {
      throw new IllegalStateException("Model for '" + this.toString() + "' (a '"
          + getModel(NamedObject.class).map(NamedObject::getName) + ")' is not parameterizable.");
    }

    getModel(ParameterizedModel.class)
        .ifPresent(parameterizedModel -> {
          getModel(SourceModel.class)
              // For sources, we need to account for the case where parameters in the callbacks may have colliding names.
              // This logic ensures that the parameter fetching logic is consistent with the logic that handles this scenario in
              // previous implementations.
              .map(sourceModel -> Stream
                  .concat(parameterizedModel.getParameterGroupModels().stream(),
                          Stream.concat(sourceModel.getSuccessCallback().map(cb -> cb.getParameterGroupModels().stream())
                              .orElse(Stream.empty()),
                                        sourceModel.getErrorCallback().map(cb -> cb.getParameterGroupModels().stream())
                                            .orElse(Stream.empty()))))
              .orElse(parameterizedModel.getParameterGroupModels().stream())
              .forEach(pg -> {
                if (pg.isShowInDsl()) {
                  final Optional<ComponentAst> paramGroupComp = directChildrenStream()
                      // Comparing the group model name with AST hyphenized name
                      // TODO: this should be compared with a resolved DSLElementSyntax from the extension model.
                      .filter(comp -> hyphenize(pg.getName()).equals(comp.getIdentifier().getName()))
                      .findAny();

                  if (paramGroupComp.isPresent()) {
                    pg.getParameterModels()
                        .forEach(paramModel -> populateParameterAst(paramGroupComp.get()
                            .getRawParameterValue(paramModel.getName()), paramModel));
                  } else {
                    pg.getParameterModels()
                        .forEach(paramModel -> populateParameterAst(empty(), paramModel));
                  }
                } else {
                  pg.getParameterModels().forEach(paramModel -> {
                    final ComponentParameterAst computedParam =
                        populateParameterAst(this.getRawParameterValue(paramModel.getName()), paramModel);
                    if (paramModel.isComponentId()) {
                      componentId = (String) computedParam.getValue().getRight();
                    }
                  });
                }

              });
        });
  }

  private ComponentParameterAst populateParameterAst(Optional<String> rawValue, ParameterModel paramModel) {
    return parameterAsts.computeIfAbsent(paramModel.getName(),
                                         paramNameKey -> rawValue
                                             .map(rawParamValue -> new DefaultComponentParameterAst(rawParamValue,
                                                                                                    () -> paramModel))
                                             .orElseGet(() -> new DefaultComponentParameterAst(null,
                                                                                               () -> paramModel)));
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
        if (!rootProcessed) {
          rootProcessed = true;
          action.accept(SpringComponentModel.this);
          return true;
        }

        trySplit();

        if (currentChildSpliterator != null) {
          if (currentChildSpliterator.tryAdvance(action)) {
            return true;
          } else {
            currentChildSpliterator = null;
            return tryAdvance(action);
          }
        } else {
          if (innerSpliterator.tryAdvance(cm -> {
            currentChildSpliterator = cm.recursiveSpliterator();
          })) {
            return tryAdvance(action);
          } else {
            return false;
          }
        }
      }

      @Override
      public Spliterator<ComponentAst> trySplit() {
        if (innerSpliterator == null) {
          innerSpliterator = directChildrenStream().spliterator();
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

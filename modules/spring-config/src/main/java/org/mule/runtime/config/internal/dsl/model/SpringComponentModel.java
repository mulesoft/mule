/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ComponentModel;

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
  public Optional<String> getName() {
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
          innerSpliterator = getInnerComponents().stream().map(ic -> (ComponentAst) ic).spliterator();
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
          innerSpliterator = getInnerComponents().stream().map(ic -> (ComponentAst) ic).spliterator();
        }
        // return innerSpliterator.trySplit();
        return null;
      }

      @Override
      public long estimateSize() {
        return 1 + getInnerComponents().stream()
            .mapToLong(inner -> ((ComponentAst) inner).recursiveSpliterator().estimateSize())
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
    return getInnerComponents().stream()
        .map(cm -> (ComponentAst) cm);
  }

  @Override
  public Spliterator<ComponentAst> directChildrenSpliterator() {
    return directChildrenStream().spliterator();
  }
}

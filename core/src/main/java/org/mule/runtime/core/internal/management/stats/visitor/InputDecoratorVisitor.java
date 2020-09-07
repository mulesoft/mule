/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats.visitor;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.runtime.core.internal.management.stats.InputDecoratedCursorStreamProvider;

/**
 * Visitor that returns a decorator for input statistics.
 * 
 * @since 4.4, 4.3.1
 */
public class InputDecoratorVisitor<T> implements Visitor<T> {

  private final CursorComponentDecoratorFactory decoratorFactory;
  private final String correlationId;

  private InputDecoratorVisitor(CursorComponentDecoratorFactory decoratorFactory,
                                String correlationId) {
    this.decoratorFactory = decoratorFactory;
    this.correlationId = correlationId;
  }

  @Override
  public InputStream visitInputStream(VisitableInputStream visitable) {
    return decoratorFactory.decorateInput(visitable, correlationId);
  }

  @Override
  public Iterator<T> visitIterator(VisitableIterator<T> visitable) {
    return decoratorFactory.decorateInput(visitable.getDelegate(), correlationId);
  }

  @Override
  public Collection<T> visitCollection(VisitableCollection<T> visitable) {
    return decoratorFactory.decorateInput(visitable.getDelegate(), correlationId);
  }

  @Override
  public List<T> visitList(VisitableList<T> visitableList) {
    return (List<T>) decoratorFactory.decorateInput(visitableList, correlationId);
  }

  @Override
  public Set<T> visitSet(VisitableSet<T> visitableSet) {
    return (Set<T>) decoratorFactory.decorateInput(visitableSet, correlationId);
  }


  @Override
  public CursorStreamProvider visitCursorStreamProvider(VisitableCursorStreamProvider cursorStreamProvider) {
    return new InputDecoratedCursorStreamProvider(cursorStreamProvider.getDelegate(), decoratorFactory, correlationId);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private CursorComponentDecoratorFactory factory;
    private String correlationId;

    private Builder() {}

    public Builder withFactory(CursorComponentDecoratorFactory factory) {
      this.factory = factory;
      return this;
    }

    public Builder withCorrelationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public InputDecoratorVisitor build() {
      return new InputDecoratorVisitor(factory, correlationId);
    }

  }

}

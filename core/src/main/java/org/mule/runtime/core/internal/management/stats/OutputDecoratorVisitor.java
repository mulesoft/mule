/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.runtime.core.internal.management.stats.InputDecoratorVisitor.Builder;

/**
 * Visitor that returns a decorator for output statistics.
 * 
 * @since 4.4, 4.3.1
 */
public class OutputDecoratorVisitor implements Visitor {

  private final CursorComponentDecoratorFactory decoratorFactory;
  private final String correlationId;
  private boolean decorateInputStreams = true;
  private boolean decorateIterators = true;
  private boolean decorateCollections = true;
  private boolean decorateCursorProviders = true;

  private OutputDecoratorVisitor(CursorComponentDecoratorFactory decoratorFactory, String correlationId,
                                 boolean decorateInputStreams, boolean decorateIterators, boolean decorateCollections,
                                 boolean decorateCursorProviders) {
    this.decoratorFactory = decoratorFactory;
    this.correlationId = correlationId;
    this.decorateInputStreams = decorateInputStreams;
    this.decorateIterators = decorateIterators;
    this.decorateCollections = decorateCollections;
    this.decorateCursorProviders = decorateCursorProviders;
  }

  @Override
  public InputStream visitInputStream(VisitableInputStream visitable) {
    return decoratorFactory.decorateOutput(visitable.getDelegate(), correlationId);
  }

  @Override
  public Iterator visitIterator(VisitableIterator visitable) {
    return decoratorFactory.decorateOutput(visitable.getDelegate(), correlationId);
  }

  @Override
  public Collection visitCollection(VisitableCollection visitable) {
    return decoratorFactory.decorateOutputResultCollection(visitable.getDelegate(), correlationId);
  }

  @Override
  public List visitList(VisitableList visitableList) {
    return visitableList;
  }

  @Override
  public Set visitSet(VisitableSet visitableSet) {
    return visitableSet;
  }

  @Override
  public CursorStreamProvider visitCursorStreamProvider(VisitableCursorStreamProvider visitableCursorStreamProvider) {
    return visitableCursorStreamProvider;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private boolean decorateCollections = true;
    private boolean decorateIterators = true;
    private boolean decorateInputStreams = true;
    private boolean decorateCursorProviders = true;

    private CursorComponentDecoratorFactory factory;
    private String correlationId;

    private Builder() {}

    public Builder decorateCollections(boolean decorateCollections) {
      this.decorateCollections = decorateCollections;
      return this;
    }

    public Builder decorateIterators(boolean decorateIterators) {
      this.decorateIterators = decorateIterators;
      return this;
    }

    public Builder decorateInputStreams(boolean decorateInputStreams) {
      this.decorateInputStreams = decorateInputStreams;
      return this;
    }

    public Builder decorateCursorProviders(boolean decorateCursorProviders) {
      this.decorateCursorProviders = decorateCursorProviders;
      return this;
    }

    public Builder withFactory(CursorComponentDecoratorFactory factory) {
      this.factory = factory;
      return this;
    }

    public Builder withCorrelationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public OutputDecoratorVisitor build() {
      return new OutputDecoratorVisitor(factory, correlationId, decorateInputStreams, decorateIterators, decorateCollections,
                                        decorateCursorProviders);
    }

  }

}

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

/**
 * Visitor that returns a decorator for input statistics.
 * 
 * @since 4.4, 4.3.1
 */
public class InputDecoratorVisitor implements Visitor {

  private final CursorComponentDecoratorFactory decoratorFactory;
  private final String correlationId;
  private boolean decorateInputStreams = true;
  private boolean decorateIterators = true;
  private boolean decorateCollections = true;
  private boolean decorateCursorProviders = true;

  private InputDecoratorVisitor(CursorComponentDecoratorFactory decoratorFactory,
                                String correlationId,
                                boolean decorateInputStreams,
                                boolean decorateIterators,
                                boolean decorateCollections,
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
    if (decorateInputStreams) {
      return decoratorFactory.decorateInput(visitable, correlationId);
    } else {
      return visitable.getDelegate();
    }
  }

  @Override
  public Iterator visitIterator(VisitableIterator visitable) {
    if (decorateIterators) {
      return decoratorFactory.decorateInput(visitable.getDelegate(), correlationId);
    } else {
      return visitable.getDelegate();
    }
  }

  @Override
  public Collection visitCollection(VisitableCollection visitable) {
    if (decorateCollections) {
      return decoratorFactory.decorateInput(visitable.getDelegate(), correlationId);
    } else {
      return visitable.getDelegate();
    }
  }

  @Override
  public List visitList(VisitableList visitableList) {
    if (decorateCollections) {
      return (List) decoratorFactory.decorateInput(visitableList, correlationId);
    }
    return visitableList.getDelegate();
  }

  @Override
  public Set visitSet(VisitableSet visitableSet) {
    if (decorateCollections) {
      return (Set) decoratorFactory.decorateInput(visitableSet, correlationId);
    }
    return visitableSet.getDelegate();
  }


  @Override
  public CursorStreamProvider visitCursorStreamProvider(VisitableCursorStreamProvider cursorStreamProvider) {
    if (decorateCursorProviders) {
      return new InputDecoratedCursorStreamProvider(cursorStreamProvider.getDelegate(), decoratorFactory, correlationId);
    }

    return cursorStreamProvider.getDelegate();
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

    public InputDecoratorVisitor build() {
      return new InputDecoratorVisitor(factory, correlationId, decorateInputStreams, decorateIterators, decorateCollections,
                                       decorateCursorProviders);
    }

  }

}

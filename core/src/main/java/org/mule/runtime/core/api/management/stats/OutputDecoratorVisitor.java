/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Visitor that returns a decorator for output statistics.
 * 
 * @since 4.4, 4.3.1
 */
public class OutputDecoratorVisitor implements Visitor {

  private final CursorComponentDecoratorFactory decoratorFactory;
  private final String correlationId;

  public OutputDecoratorVisitor(CursorComponentDecoratorFactory decoratorFactory, String correlationId) {
    this.decoratorFactory = decoratorFactory;
    this.correlationId = correlationId;
  }

  @Override
  public InputStream visitInputStream(VisitableInputStream visitable) {
    return decoratorFactory.decorateOutput(visitable, correlationId);
  }

  @Override
  public Iterator visitIterator(VisitableIterator visitable) {
    return decoratorFactory.decorateOutput(visitable, correlationId);
  }

  @Override
  public Collection visitCollection(VisitableCollection visitable) {
    return visitable;
  }

  @Override
  public List visitList(VisitableList visitableList) {
    return visitableList;
  }

  @Override
  public Set visitSet(VisitableSet visitableSet) {
    return visitableSet;
  }

}

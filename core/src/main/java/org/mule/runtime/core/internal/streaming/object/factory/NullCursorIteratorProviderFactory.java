/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import static org.mule.runtime.core.api.functional.Either.right;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.streaming.CursorManager;

import java.util.Iterator;

public class NullCursorIteratorProviderFactory extends AbstractCursorIteratorProviderFactory {

  public NullCursorIteratorProviderFactory(CursorManager cursorManager) {
    super(cursorManager);
  }

  @Override
  protected Either<CursorIteratorProvider, Iterator> resolve(Iterator iterator, Event event) {
    return right(iterator);
  }
}

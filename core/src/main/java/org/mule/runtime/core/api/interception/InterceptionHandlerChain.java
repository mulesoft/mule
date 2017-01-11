/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.interception;

import static java.util.Collections.unmodifiableList;

import org.mule.runtime.api.interception.InterceptionHandler;

import java.util.List;
import java.util.ListIterator;

/**
 * TODO should not be API
 */
public class InterceptionHandlerChain {

  private List<InterceptionHandler> interceptionHandlers;

  public InterceptionHandlerChain(List<InterceptionHandler> interceptionHandlers) {
    this.interceptionHandlers = unmodifiableList(interceptionHandlers);
  }

  public ListIterator<InterceptionHandler> listIterator() {
    return interceptionHandlers.listIterator();
  }
}

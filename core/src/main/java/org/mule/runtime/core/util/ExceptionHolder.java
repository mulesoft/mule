/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** TODO */
public class ExceptionHolder implements ExceptionListener {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private List<Exception> exceptions = new ArrayList<Exception>(2);

  public void exceptionThrown(Exception e) {
    exceptions.add(e);
  }

  public List getExceptions() {
    return exceptions;
  }

  public boolean isExceptionThrown() {
    return exceptions.size() > 0;
  }

  public void clear() {
    exceptions.clear();
  }

  public void print() {
    for (Exception exception : exceptions) {
      logger.error("Exception thrown", exception);
    }
  }
}

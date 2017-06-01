/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component.simple;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;

/**
 * A service that will return a static data object as a result. This is useful for testing with expected results. The data
 * returned can be read from a file or set as a property on this service.
 */
public class StaticComponent implements Callable, Initialisable, MuleContextAware {

  private Object data;
  private String dataFile;
  private String prefix;
  private String postfix;
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    if (dataFile != null) {
      try {
        data = IOUtils.getResourceAsString(dataFile, getClass());
      } catch (IOException e) {
        throw new InitialisationException(e, this);
      }
    }
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public String getDataFile() {
    return dataFile;
  }

  public void setDataFile(String dataFile) {
    this.dataFile = dataFile;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getPostfix() {
    return postfix;
  }

  public void setPostfix(String postfix) {
    this.postfix = postfix;
  }

  @Override
  public Object onCall(MuleEventContext eventContext) throws Exception {
    if (data != null) {
      return data;
    }

    String eventData = eventContext.transformMessageToString(muleContext);

    if (prefix != null) {
      eventData = prefix + eventData;
    }

    if (postfix != null) {
      eventData += postfix;
    }

    return eventData;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}

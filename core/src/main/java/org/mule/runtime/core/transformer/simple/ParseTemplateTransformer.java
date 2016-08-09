/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.IOUtils;

import java.nio.charset.Charset;

/**
 * Loads a template and parses its content to resolve expressions. The order in which attempts to load the resource is the
 * following: from the file system, from a URL or from the classpath.
 */
public class ParseTemplateTransformer extends AbstractMessageTransformer {

  private String location;
  private String template;

  public ParseTemplateTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    loadTemplate();
  }

  private void loadTemplate() throws InitialisationException {
    try {
      if (location == null) {
        throw new IllegalArgumentException("Location cannot be null");
      }
      template = IOUtils.getResourceAsString(location, this.getClass());

    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }


  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    if (template == null) {
      throw new IllegalArgumentException("Template cannot be null");
    }

    return muleContext.getExpressionManager().parse(template, event);
  }

  public void setLocation(String location) {
    this.location = location;
  }
}

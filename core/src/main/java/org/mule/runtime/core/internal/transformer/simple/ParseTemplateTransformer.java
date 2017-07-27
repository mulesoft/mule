/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.message.DefaultEventBuilder;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.api.util.IOUtils;

import java.nio.charset.Charset;

/**
 * Loads a template and parses its content to resolve expressions. The order in which attempts to load the resource is the
 * following: from the file system, from a URL or from the classpath.
 */
public class ParseTemplateTransformer extends AbstractMessageTransformer {

  private String content;
  private String encoding;
  private String target;

  public ParseTemplateTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    //loadTemplate();
  }

  //private void loadTemplate() throws InitialisationException {
  //  try {
  //    if (location == null) {
  //      throw new IllegalArgumentException("Location cannot be null");
  //    }
  //    template = IOUtils.getResourceAsString(location, this.getClass());
  //
  //  } catch (Exception e) {
  //    throw new InitialisationException(e, this);
  //  }
  //}


  @Override
  public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
    if (content == null) {
      throw new IllegalArgumentException("Template cannot be null");
    }
    return muleContext.getExpressionManager().parse(content, event, null);
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

}

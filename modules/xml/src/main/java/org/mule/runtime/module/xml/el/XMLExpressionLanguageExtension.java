/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.el;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExpressionLanguageContext;
import org.mule.runtime.core.api.el.ExpressionLanguageExtension;
import org.mule.runtime.core.api.el.ExpressionLanguageFunction;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

public class XMLExpressionLanguageExtension extends org.mule.runtime.core.el.mvel.DataConversion
    implements ExpressionLanguageExtension, MuleContextAware, Initialisable {

  private MuleContext muleContext;
  private XMLToStringConversionHandler conversionHandler = new XMLToStringConversionHandler();
  private ExpressionLanguageFunction xpath3Function;

  @Override
  public void initialise() throws InitialisationException {
    xpath3Function = new XPath3Function(muleContext);
  }

  @Override
  public void configureContext(ExpressionLanguageContext context) {
    addConversionHandler(String.class, conversionHandler);
    context.declareFunction("xpath3", xpath3Function);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}

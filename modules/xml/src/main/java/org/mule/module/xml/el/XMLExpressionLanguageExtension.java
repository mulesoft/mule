/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.el;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

public class XMLExpressionLanguageExtension extends org.mule.el.mvel.DataConversion
        implements ExpressionLanguageExtension, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private XMLToStringConversionHandler conversionHandler = new XMLToStringConversionHandler();
    private ExpressionLanguageFunction xpathFunction;
    private ExpressionLanguageFunction xpath3Function;

    @Override
    public void initialise() throws InitialisationException
    {
        xpathFunction = new XPathFunction(muleContext);
        xpath3Function = new XPath3Function(muleContext);
    }

    @Override
    public void configureContext(ExpressionLanguageContext context)
    {
        addConversionHandler(String.class, conversionHandler);
        context.declareFunction("xpath", xpathFunction);
        context.declareFunction("xpath3", xpath3Function);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    @Override
    public void initialise() throws InitialisationException
    {
        xpathFunction = new XPathFunction(muleContext);
    }

    @Override
    public void configureContext(ExpressionLanguageContext context)
    {
        addConversionHandler(String.class, conversionHandler);
        context.declareFunction("xpath", xpathFunction);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}

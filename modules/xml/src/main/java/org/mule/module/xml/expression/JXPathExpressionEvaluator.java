/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.i18n.XmlMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;

import java.util.Map;

import org.apache.commons.jxpath.Container;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * Will extract properties based on Xpath expressions. Will work on Xml/Dom and beans
 *
 * @deprecated Developers should use xpath, bean or groovy instead of this expression evaluator since there are some
 * quirks with JXPath and the performance is not good.
 */
@Deprecated
public class JXPathExpressionEvaluator implements ExpressionEvaluator, MuleContextAware
{
    public static final String NAME = "jxpath";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    protected transient MuleContext muleContext;
    private NamespaceManager namespaceManager;

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    @Override
    public Object evaluate(String expression, MuleMessage message)
    {
        Document document;
        try
        {
            document = XMLUtils.toW3cDocument(message.getPayload());
        }
        catch (Exception e)
        {
            logger.error(e);
            return null;
        }

        JXPathContext context;

        if (document != null)
        {
            context = createContextForXml(document);
        }
        else
        {
            context = createContextForBean(message.getPayload());
        }

        return getExpressionValue(context, expression);
    }

    private JXPathContext createContextForXml(final Document document)
    {
        Container container = new Container()
        {

            @Override
            public Object getValue()
            {
                return document;
            }

            @Override
            public void setValue(Object value)
            {
                throw new UnsupportedOperationException();
            }
        };

        return JXPathContext.newContext(container);
    }

    private JXPathContext createContextForBean(Object payload)
    {
        return JXPathContext.newContext(payload);
    }

    private Object getExpressionValue(JXPathContext context, String expression)
    {
        NamespaceManager theNamespaceManager = getNamespaceManager();
        if (theNamespaceManager != null)
        {
            addNamespacesToContext(theNamespaceManager, context);
        }

        Object result = null;

        try
        {
            result = context.getValue(expression);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("failed to process JXPath expression: " + expression, e);
            }
        }

        return result;
    }

    protected void addNamespacesToContext(NamespaceManager manager, JXPathContext context)
    {
        for (Map.Entry<String, String> entry : manager.getNamespaces().entrySet())
        {
            try
            {
                context.registerNamespace(entry.getKey(), entry.getValue());
            }
            catch (Exception e)
            {
                throw new ExpressionRuntimeException(XmlMessages.failedToRegisterNamespace(entry.getKey(), entry.getValue()));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    /**
     *
     * @return the nsmespace manager from the registry
     */
    protected synchronized NamespaceManager getNamespaceManager()
    {
        if (namespaceManager == null)
        {

            try
            {
                // We defer looking this up until registry is completely built
                if (muleContext != null)
                {
                    namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
                }
            }
            catch (RegistrationException e)
            {
                throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
            }
        }
        return namespaceManager;
    }
}

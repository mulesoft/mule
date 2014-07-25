/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.sxc;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.module.xml.transformer.XmlToXMLStreamReader;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.logic.OrFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;

import com.envoisolutions.sxc.xpath.XPathBuilder;
import com.envoisolutions.sxc.xpath.XPathEvaluator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.LoggerFactory;

/**
 * <code>FilteringRouter</code> is a router that accepts events based on a filter
 * set.
 */

public class SxcFilteringOutboundRouter extends FilteringOutboundRouter
{
    private final static ThreadLocal<MuleMessage> messages = new ThreadLocal<MuleMessage>();

    private XmlToXMLStreamReader transformer = new XmlToXMLStreamReader();

    private Map<String, String> namespaces;
    private XPathEvaluator evaluator;

    private XPathBuilder builder;

    private NamespaceManager namespaceManager;

    static
    {
        LoggerFactory.getLogger(SxcFilter.class).warn("SXC module is deprecated and will be removed in Mule 4.0.");
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
        }
        catch (RegistrationException e)
        {
            throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
        }

        if (namespaceManager != null)
        {
            if (namespaces == null)
            {
                namespaces = new HashMap<String, String>(namespaceManager.getNamespaces());
            }
            else
            {
                namespaces.putAll(namespaceManager.getNamespaces());
            }
        }
    }

    @Override
    public void setFilter(Filter filter)
    {
        super.setFilter(filter);
    }

    protected void addEventHandlers(XPathBuilder xpathBuilder, Filter filter)
    {
        if (filter instanceof SxcFilter)
        {
            SxcFilter sxcFilter = ((SxcFilter) filter);
            sxcFilter.addEventHandler(this, xpathBuilder);
        }
        else if (filter instanceof AndFilter)
        {
            AndFilter f = (AndFilter) filter;

            for (Iterator<?> itr = f.getFilters().iterator(); itr.hasNext();)
            {
                addEventHandlers(xpathBuilder, (Filter) itr.next());
            }
        }
        else if (filter instanceof OrFilter)
        {
            OrFilter f = (OrFilter) filter;

            for (Iterator<?> itr = f.getFilters().iterator(); itr.hasNext();)
            {
                addEventHandlers(xpathBuilder, (Filter) itr.next());
            }
        }
        else if (filter instanceof NotFilter)
        {
            NotFilter f = (NotFilter) filter;

            addEventHandlers(xpathBuilder, f.getFilter());
        }
        else
        {
            logger.warn("Filter type " + filter.getClass().toString()
                           + " is not recognized by the SXC router. If it contains child "
                           + "SXC filters it will not work correctly.");
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();

        transformer.setReversible(true);
        transformer.initialise();

        builder = new XPathBuilder();
        builder.setNamespaceContext(namespaces);
        addEventHandlers(builder, getFilter());

        evaluator = builder.compile();
    }

    @Override
    public boolean isMatch(MuleMessage message) throws MuleException
    {
        ReversibleXMLStreamReader reader = null;
        try
        {
            messages.set(message);

            reader = getXMLStreamReader(message);
            reader.setTracking(true);
            evaluator.evaluate(reader);
        }
        catch (StopProcessingException e)
        {
            // stop processing
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
        finally
        {
            messages.set(null);

            if (reader != null)
            {
                reader.setTracking(false);
                reader.reset();
            }
        }

        try
        {
            return testMatch(message);
        }
        catch (UndefinedMatchException m)
        {
            return false;
        }
    }

    public boolean testMatch(MuleMessage message) throws MuleException
    {
        return super.isMatch(message);
    }

    /**
     * Gets an XMLStreamReader for this message.
     *
     * @param message
     * @throws TransformerException
     */
    protected ReversibleXMLStreamReader getXMLStreamReader(MuleMessage message) throws TransformerException
    {
         ReversibleXMLStreamReader r = (ReversibleXMLStreamReader) transformer.transform(message);

         if (r != message.getPayload())
         {
             message.setPayload(r);
         }
         return r;
    }

    public Map<String, String> getNamespaces()
    {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces)
    {
        this.namespaces = namespaces;
    }

    public static MuleMessage getCurrentMessage()
    {
        return messages.get();
    }
}

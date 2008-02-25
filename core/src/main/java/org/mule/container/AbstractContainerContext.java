/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.container;

import org.mule.api.MuleContext;
import org.mule.api.context.ContainerContext;
import org.mule.api.context.ContainerException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.util.ChainedReader;
import org.mule.util.SystemUtils;

import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractContainerContext</code> provides base container configuration
 * functions for handling embedded configuration.
 */
public abstract class AbstractContainerContext implements ContainerContext, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private String name;

    protected MuleContext muleContext;

    protected AbstractContainerContext(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    // default implementation does nothing
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return LifecycleTransitionResult.OK;
    }

    public final void doInitialise(MuleContext muleContext) throws InitialisationException
    {
        
    }

    public void dispose()
    {
        // noop
    }

    public final void configure(Reader configuration, String doctype, String encoding)
        throws ContainerException
    {
        String decl = getXmlDeclaration(encoding);
        logger.debug("Using Xml declaration: " + decl);
        if (doctype == null)
        {
            doctype = getDefaultDocType();
        }
        if (doctype != null)
        {
            if (!doctype.startsWith("<!DOCTYPE"))
            {
                doctype = "<!DOCTYPE " + doctype + ">";
            }
            logger.info("Using doctype: " + doctype);
        }
        else
        {
            doctype = "";
        }
        StringReader declaration = new StringReader(decl + SystemUtils.LINE_SEPARATOR + doctype);
        ChainedReader reader = new ChainedReader(declaration, configuration);
        configure(reader);

    }

    protected String getXmlDeclaration(String encoding)
    {
        if (encoding == null)
        {
            encoding = getDefaultEncoding();
        }
        return "<?xml version=\"1.0\" encoding=\"" + encoding.toUpperCase() + "\"?>";
    }

    protected String getDefaultDocType()
    {
        return null;
    }

    protected String getDefaultEncoding()
    {
        return muleContext.getConfiguration().getDefaultEncoding();
    }


    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public abstract void configure(Reader configuration) throws ContainerException;
}

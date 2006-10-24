/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.container;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.util.ChainedReader;

import java.io.Reader;
import java.io.StringReader;

/**
 * <code>AbstractContainerContext</code> provides base container configuration
 * functions for handling embedded configuration
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractContainerContext implements UMOContainerContext
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private String name;

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

    public void initialise() throws InitialisationException
    {
        // noop
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
        return MuleManager.getConfiguration().getEncoding();
    }

    public abstract void configure(Reader configuration) throws ContainerException;
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.streaming;

import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOStreamingTransformer;
import org.mule.util.ClassUtils;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public abstract class AbstractStreamingTransformer implements UMOStreamingTransformer
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    /**
     * The name that identifies this transformer. If none is set the class name of
     * the transformer is used
     */
    protected String name = null;

    /**
     * The endpoint that this transformer instance is configured on
     */
    protected UMOImmutableEndpoint endpoint = null;

    /**
     * This is the following transformer in the chain of transformers.
     */
    protected UMOStreamingTransformer nextTransformer;

    /**
     * Determines whether the transformer will throw an exception if the message
     * passed is is not supported or the return tye is incorrect
     */
    private boolean ignoreBadInput = false;

    /**
     * default constructor required for discovery
     */
    public AbstractStreamingTransformer()
    {
        name = generateTransformerName();
    }

    /**
     * @return transformer name
     */
    public String getName()
    {
        if (name == null)
        {
            setName(ClassUtils.getShortClassName(getClass()));
        }
        return name;
    }

    /**
     * @param string
     */
    public void setName(String string)
    {
        logger.debug("Setting transformer name to: " + name);
        name = string;
    }

    /**
     * Transforms the object.
     * 
     * @param src The source object to transform.
     * @return The transformed object
     */
    public final Object transform(InputStream src, OutputStream dest, String encoding)
        throws TransformerException
    {
        if (encoding == null && endpoint != null)
        {
            encoding = endpoint.getEncoding();
        }

        // last resort
//        if (encoding == null)
//        {
//            encoding = RegistryContext.getConfiguration().getDefaultEncoding();
//        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Applying transformer " + getName() + " (" + getClass().getName() + ")");
            logger.debug("Object before transform");
        }

        // TODO we should have a pipeline of transformers that we just 'execute'
        // Object result = doTransform(src, dest, encoding);
        // if (result == null)
        // {
        // result = NullPayload.getInstance();
        // }
        //
        // if (logger.isDebugEnabled())
        // {
        // logger.debug("Object after transform");
        // }
        //
        // if (nextTransformer != null)
        // {
        // logger.debug("Following transformer in the chain is " +
        // nextTransformer.getName() + " ("
        // + nextTransformer.getClass().getName() + ")");
        // result = nextTransformer.transform(result);
        // }
        //
        // return result;
        return null;
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.transformer.UMOTransformer#setConnector(org.mule.umo.provider.UMOConnector)
     */
    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        UMOStreamingTransformer trans = nextTransformer;
        while (trans != null && endpoint != null)
        {
            trans.setEndpoint(endpoint);
            trans = trans.getNextTransformer();
        }
    }

    protected abstract Object doTransform(InputStream src, OutputStream dest, String encoding)
        throws TransformerException;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.transformer.UMOTransformer#getNextTransformer()
     */
    public UMOStreamingTransformer getNextTransformer()
    {
        return nextTransformer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.transformer.UMOTransformer#setNextTransformer(org.mule.umo.transformer.UMOTransformer)
     */
    public void setNextTransformer(UMOStreamingTransformer nextTransformer)
    {
        this.nextTransformer = nextTransformer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        try
        {
            return BeanUtils.cloneBean(this);
        }
        catch (Exception e)
        {
            throw new CloneNotSupportedException("Failed to clone transformer: " + e.getMessage());
        }
    }

    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     * 
     * @throws org.mule.umo.lifecycle.InitialisationException
     * @param managementContext
     */
    public void initialise(UMOManagementContext managementContext) throws InitialisationException
    {
        // nothing to do
    }

    protected String generateTransformerName()
    {
        String name = getClass().getName();
        int i = name.lastIndexOf(".");
        if (i > -1)
        {
            name = name.substring(i + 1);
        }
        return name;
    }

    public boolean isIgnoreBadInput()
    {
        return ignoreBadInput;
    }

    public void setIgnoreBadInput(boolean ignoreBadInput)
    {
        this.ignoreBadInput = ignoreBadInput;
    }

    public String toString()
    {
        return "StreamingTransformer{" + "name='" + name + "'" + ", ignoreBadInput=" + ignoreBadInput + "}";
    }

    public boolean isAcceptNull()
    {
        return false;
    }

}

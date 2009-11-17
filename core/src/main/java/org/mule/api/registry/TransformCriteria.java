/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

/**
 * A bean that holds information about a transformer to be discovered in the Registry.  {@link TransformerResolver}
 * instances will use some or all of this information to select a transformer that matches the criteria.
 */
public class TransformCriteria
{
    private Class[] inputTypes;
    private Class outputType;
    private String[] inboundMimeTypes;
    private String outboundMimeType;

    public TransformCriteria()
    {
    }

    public TransformCriteria(Class[] inputTypes, Class outputType, String[] inboundMimeTypes, String outboundMimeType)
    {
        this.inputTypes = inputTypes;
        this.outputType = outputType;
        this.inboundMimeTypes = inboundMimeTypes;
        this.outboundMimeType = outboundMimeType;
    }

    public Class[] getInputTypes()
    {
        return inputTypes;
    }

    public void setInputTypes(Class[] inputTypes)
    {
        this.inputTypes = inputTypes;
    }

    public Class getOutputType()
    {
        return outputType;
    }

    public void setOutputType(Class outputType)
    {
        this.outputType = outputType;
    }

    /**
     * Not currently supported by the Mule 3.0 api
     */
    public String[] getInboundMimeTypes()
    {
        return inboundMimeTypes;
    }

    /**
     * Not currently supported by the Mule 3.0 api
     */
    public void setInboundMimeTypes(String[] inboundMimeTypes)
    {
        this.inboundMimeTypes = inboundMimeTypes;
    }

    /**
     * Not currently supported by the Mule 3.0 api
     */
    public String getOutboundMimeType()
    {
        return outboundMimeType;
    }

    /**
     * Not currently supported by the Mule 3.0 api
     */
    public void setOutboundMimeType(String outboundMimeType)
    {
        this.outboundMimeType = outboundMimeType;
    }
}

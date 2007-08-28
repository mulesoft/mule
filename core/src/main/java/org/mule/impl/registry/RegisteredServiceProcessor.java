/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.registry;

import org.mule.impl.model.ModelFactory;
import org.mule.umo.UMODescriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public class RegisteredServiceProcessor implements ObjectProcessor
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(RegisteredServiceProcessor.class);

    public Object process(Object object)
    {
        if(object instanceof UMODescriptor)
        {
            UMODescriptor descriptor = (UMODescriptor)object;
            if(descriptor.getModelName()==null)
            {
                logger.warn("modelName proeprty not set on service: " + descriptor.getName() + " trying default model name: " + ModelFactory.DEFAULT_MODEL_NAME);
                descriptor.setModelName(ModelFactory.DEFAULT_MODEL_NAME);
            }
        }
        return object;
    }
}
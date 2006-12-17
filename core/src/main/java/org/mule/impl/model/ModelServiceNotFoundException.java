/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.config.i18n.Message;
import org.mule.umo.model.ModelException;

/**
 * Is thrown when a model service name is specified and cannot be found or loaded
 */
public class ModelServiceNotFoundException extends ModelException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5306713972201495210L;

    /**
     * @param location the path of the service
     */
    public ModelServiceNotFoundException(String location)
    {
        super(Message.createStaticMessage(location));
    }

    /**
     * @param location the path of the service
     * @param cause the exception that cause this exception to be thrown
     */
    public ModelServiceNotFoundException(String location, Throwable cause)
    {
        super(Message.createStaticMessage(location), cause);
    }
}

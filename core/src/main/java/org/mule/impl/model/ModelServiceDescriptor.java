/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceException;
import org.mule.umo.model.UMOModel;

/**
 * <code>ModelServiceDescriptor</code> describes the necessery information for
 * creating a model from a service descriptor. A service descriptor should be
 * located at META-INF/services/org/mule/models/<type> where type is the
 * type of the model to be created.  The service descriptor is in the form of
 * string key value pairs and supports a number of properties, descriptions of which
 * can be found here: http://www.muledocs.org/Model+Service+Descriptors.
 */
public interface ModelServiceDescriptor extends ServiceDescriptor
{
    public UMOModel createModel() throws ServiceException;

    public Class getModelClass() throws ServiceException;
}



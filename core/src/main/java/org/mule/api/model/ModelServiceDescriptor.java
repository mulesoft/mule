/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.model;

import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceException;

/**
 * <code>ModelServiceDescriptor</code> describes the necessery information for
 * creating a model from a service descriptor. A service descriptor should be
 * located at META-INF/services/org/mule/models/<type> where type is the
 * type of the model to be created.  The service descriptor is in the form of
 * string key value pairs.
 */
@Deprecated
public interface ModelServiceDescriptor extends ServiceDescriptor
{
    Model createModel() throws ServiceException;

    Class<Model> getModelClass() throws ServiceException;
}

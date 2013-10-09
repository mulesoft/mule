/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
public interface ModelServiceDescriptor extends ServiceDescriptor
{
    Model createModel() throws ServiceException;

    Class<Model> getModelClass() throws ServiceException;
}

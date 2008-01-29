/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleException;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.service.Service;
import org.mule.lifecycle.DefaultLifecycleAdapter;

/** <code>TestDefaultLifecycleAdapter</code> TODO document */
public class TestDefaultLifecycleAdapter extends DefaultLifecycleAdapter
{
    /**
     * @param service
     * @param descriptor
     * @throws MuleException
     */
    public TestDefaultLifecycleAdapter(Object pojoService, Service service) throws MuleException
    {
        super(pojoService, service);
    }

    /**
     * @param service
     * @param descriptor
     * @param epResolver
     * @throws MuleException
     */
    public TestDefaultLifecycleAdapter(Object pojoService,
                                       Service service,
                                       EntryPointResolverSet epResolver) throws MuleException
    {
        super(pojoService, service, epResolver);
    }

}

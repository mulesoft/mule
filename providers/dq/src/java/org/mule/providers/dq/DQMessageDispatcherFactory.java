/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.dq;

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

/**
 * @author m999svm
 * 
 * DQMessageDispatcherFactory
 */
public class DQMessageDispatcherFactory implements UMOMessageDispatcherFactory
{

    public final UMOMessageDispatcher create(final UMOImmutableEndpoint endpoint) throws UMOException
    {
        return new DQMessageDispatcher(endpoint);
    }

}

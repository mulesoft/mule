/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional;

import org.mule.extension.http.internal.HttpConnector;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.extension.socket.api.SocketsExtension;

public abstract class AbstractHttpTestCase extends ExtensionFunctionalTestCase
{
    protected static final int DEFAULT_TIMEOUT = 1000;

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {SocketsExtension.class, HttpConnector.class};
    }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.api.tls.TlsContextFactory;
import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.annotation.capability.Xml;
import org.mule.extension.api.annotation.connector.Providers;
import org.mule.extension.api.annotation.param.Optional;

import java.util.List;

@Extension(name = "petstore", description = "PetStore Test connector")
@Operations(PetStoreOperations.class)
@Providers({SimplePetStoreConnectionProvider.class, PooledPetStoreConnectionProvider.class, PoolablePetStoreConnectionProvider.class})
@Xml(schemaLocation = "http://www.mulesoft.org/schema/mule/petstore", namespace = "petstore", schemaVersion = "4.0")
public class PetStoreConnector
{

    @Parameter
    private List<String> pets;

    @Parameter
    @Optional
    private TlsContextFactory tls;

    @Parameter
    @Optional
    private ThreadingProfile threadingProfile;

    public List<String> getPets()
    {
        return pets;
    }

    public TlsContextFactory getTlsContext()
    {
        return tls;
    }

    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }
}

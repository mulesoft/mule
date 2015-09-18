/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import org.mule.api.extension.annotations.Configuration;
import org.mule.api.extension.annotations.Parameter;
import org.mule.api.extension.annotations.connector.Connector;

@Configuration(name = "config")
@Connector(PetStoreClientConnectionHandler.class)
public class PetStoreConnectorConfig extends AbstractPetStoreConfig
{

    @Parameter
    private String username;

    @Parameter
    private String password;


    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.api.connection.ConnectionException;
import org.mule.extension.api.connection.ConnectionProvider;

public class HeisenbergConnectionProvider implements ConnectionProvider<HeisenbergExtension, HeisenbergConnection>
{

    public static final String SAUL_OFFICE_NUMBER = "505-503-4455";

    @Parameter
    @Optional(defaultValue = SAUL_OFFICE_NUMBER)
    private String saulPhoneNumber;

    @Override
    public HeisenbergConnection connect(HeisenbergExtension heisenbergExtension) throws ConnectionException
    {
        return new HeisenbergConnection(saulPhoneNumber);
    }

    @Override
    public void disconnect(HeisenbergConnection heisenbergConnection)
    {

    }
}

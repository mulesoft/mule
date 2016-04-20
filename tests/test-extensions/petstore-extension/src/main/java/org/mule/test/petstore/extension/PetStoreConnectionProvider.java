/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import javax.inject.Inject;

;

public abstract class PetStoreConnectionProvider implements ConnectionProvider<PetStoreConnector, PetStoreClient>, Lifecycle
{

    public static final String USER = "john";
    public static final String PASSWORD = "doe";

    private int initialise, start, stop, dispose = 0;

    @Inject
    private MuleContext muleContext;

    @Parameter
    private String username;

    @Parameter
    @Password
    private String password;

    @Parameter
    @Optional
    private TlsContextFactory tls;

    @Parameter
    @Optional
    private ThreadingProfile threadingProfile;

    @Override
    public PetStoreClient connect(PetStoreConnector config)
    {
        return new PetStoreClient(username, password, tls, threadingProfile);
    }

    @Override
    public void disconnect(PetStoreClient connection)
    {
        if (connection != null)
        {
            connection.disconnect();
        }
    }

    @Override
    public ConnectionValidationResult validate(PetStoreClient connection)
    {
        if (connection.getUsername().equals("john") && connection.getPassword().equals("doe"))
        {
            return ConnectionValidationResult.success();
        }
        else
        {
            return ConnectionValidationResult.failure("Invalid credentials", ConnectionExceptionCode.INCORRECT_CREDENTIALS, new Exception("Invalid credentials"));
        }
    }

    @Override
    public void dispose()
    {
        dispose++;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        initialise++;
    }

    @Override
    public void start() throws MuleException
    {
        start++;
    }

    @Override
    public void stop() throws MuleException
    {
        stop++;
    }

    public int getInitialise()
    {
        return initialise;
    }

    public int getStart()
    {
        return start;
    }

    public int getStop()
    {
        return stop;
    }

    public int getDispose()
    {
        return dispose;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}

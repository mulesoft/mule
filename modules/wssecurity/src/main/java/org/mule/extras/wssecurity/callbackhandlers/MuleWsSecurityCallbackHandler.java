/*
 * $Id:
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.callbackhandlers;

import java.io.IOException;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.ws.security.WSPasswordCallback;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.manager.ObjectNotFoundException;

public class MuleWsSecurityCallbackHandler implements CallbackHandler
{
    private Properties passwords;

    /**
     * This is the standard Mule callback handler that gets a set of passwords from
     * the configuration file.
     * 
     * @throws MuleException
     */
    public MuleWsSecurityCallbackHandler() throws MuleException
    {
        PasswordContainer pass;
        try
        {
            pass = (PasswordContainer)MuleManager.getInstance().getContainerContext().getComponent(
                "passwords");
            passwords = pass.getPasswords();
        }
        catch (ObjectNotFoundException e)
        {
            throw new MuleException(new Message(Messages.AUTH_NO_CREDENTIALS), e);
        }
    }

    /**
     * This method handles the callback, i.e. it checks whether the required password
     * is available
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        for (int i = 0; i < callbacks.length; i++)
        {
            WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];

            String pass = (String)passwords.get(pc.getIdentifer());
            if (pass != null)
            {
                pc.setPassword(pass);
            }
        }
    }
}
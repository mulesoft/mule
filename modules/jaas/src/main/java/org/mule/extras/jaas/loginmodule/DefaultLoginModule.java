/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jaas.loginmodule;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * This is the Default Login Module for the Mule Jaas Authentication.
 * It extends Jaas' own LoginModule interface.
 */
public class DefaultLoginModule implements LoginModule
{

    // Callback Handlers
    private CallbackHandler callbackHandler;

    // the authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    // username and password
    private String username;
    private String password;
    private String credentials;
    private List credentialList;

    /**
     * Abort if authentication fails
     * 
     * @return boolean
     * @throws LoginException
     */
    public final boolean abort() throws LoginException
    {
        if (!succeeded)
        {
            return false;
        }
        else if (succeeded && !commitSucceeded)
        {
            // login succeeded but overall authentication failed
            succeeded = false;
            username = null;
            if (password != null)
            {
                password = null;
            }
        }
        else
        {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    /**
     * Commit if authentication succeeds, otherwise return false
     * 
     * @return boolean
     * @throws LoginException
     */
    public final boolean commit() throws LoginException
    {
        if (!succeeded)
        {
            return false;
        }
        else
        {
            // in any case, clean out state
            username = null;
            password = null;

            commitSucceeded = true;
            return true;
        }
    }

    /**
     * Initialises the callbackHandler, the credentials and the credentials list
     * 
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     */
    public final void initialize(Subject subject,
                                 CallbackHandler callbackHandler,
                                 Map sharedState,
                                 Map options)
    {
        this.callbackHandler = callbackHandler;

        this.credentials = (String)options.get("credentials");
        this.credentialList = getCredentialList(this.credentials);
    }

    /**
     * This method attempts to login the user by checking his credentials against
     * those of the authorised users.
     * 
     * @throws LoginException This is thrown either when there is no callback Handler
     *             or else when the user fails to be authenticated
     */
    public final boolean login() throws LoginException
    {
        if (callbackHandler == null)
        {
            throw new LoginException("Error: no CallbackHandler available "
                                     + "to garner authentication information from the user");
        }

        if (callbackHandler == null)
        {
            throw new LoginException("no handler");
        }

        NameCallback nameCb = new NameCallback("user: ");
        PasswordCallback passCb = new PasswordCallback("password: ", true);

        // Create the callbacks to send to the Callback Handler
        Callback[] callbacks = new Callback[]{nameCb, passCb};

        // Call the handler to get the username and password of the user.
        try
        {
            callbackHandler.handle(callbacks);
        }
        catch (IOException e)
        {
            throw new LoginException(e.toString());
        }
        catch (UnsupportedCallbackException e)
        {
            throw new LoginException("Error: " + e.getCallback().toString()
                                     + " not available to garner authentication information "
                                     + "from the user");
        }

        username = nameCb.getName();
        password = new String(passCb.getPassword());

        boolean usernameCorrect = false;
        boolean passwordCorrect = false;
        succeeded = false;

        // check the username and password against the list of authorised users
        for (int i = 0; i < credentialList.size(); i = i + 2)
        {
            if (username.equals(credentialList.get(i).toString()))
            {
                usernameCorrect = true;
            }
            else
            {
                usernameCorrect = false;
            }

            if (password.equals(credentialList.get(i + 1).toString()))
            {
                passwordCorrect = true;
            }
            else
            {
                passwordCorrect = false;
            }

            // only if both the username and password are correct will the user be
            // authenticated
            if ((usernameCorrect) & (passwordCorrect))
            {
                succeeded = true;
            }
        }

        if (succeeded)
        {
            return true;
        }
        else
        {
            succeeded = false;
            username = null;
            password = null;
            if (!usernameCorrect)
            {
                throw new FailedLoginException("User Name Incorrect");
            }
            else
            {
                throw new FailedLoginException("Password Incorrect");
            }
        }
    }

    /**
     * Returns true when authentication succeeds or false when it fails
     * 
     * @return succeeded
     */
    public final boolean logout()
    {
        return succeeded;
    }

    /**
     * This method parses the credentials string and populates the credentials list
     * against which the username and password submitted with the request will be
     * checked
     * 
     * @param credentials
     * @return outputList
     */
    public final List getCredentialList(String credentials)
    {
        boolean semicolonIsFound = false;
        boolean dividerIsFound = false;
        char[] credentialArray = credentials.toCharArray();
        String username = "";
        String password = "";
        List outputList = new Vector();

        for (int i = 0; i < credentials.length(); i++)
        {
            if ((credentialArray[i] != ':') && (!dividerIsFound))
            {
                username = username + credentialArray[i];
            }
            else if ((credentialArray[i] == ':') && (!dividerIsFound))
            {
                dividerIsFound = true;
            }
            else if ((credentialArray[i] != ';') && (!semicolonIsFound) && (dividerIsFound))
            {
                password = password + credentialArray[i];
            }
            else if ((credentialArray[i] != ';') && (!semicolonIsFound) && (dividerIsFound))
            {
                password = password + credentialArray[i];
            }
            else if ((credentialArray[i] == ';') && (!semicolonIsFound) && (dividerIsFound))
            {
                outputList.add(username);
                outputList.add(password);
                semicolonIsFound = false;
                dividerIsFound = false;
                username = "";
                password = "";
            }
        }
        return outputList;
    }
}

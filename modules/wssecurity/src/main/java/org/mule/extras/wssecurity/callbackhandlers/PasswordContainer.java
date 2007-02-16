/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.callbackhandlers;

import java.util.Properties;

public class PasswordContainer
{
    private Properties passwords;
    
    public PasswordContainer(Properties props){
        passwords = props;
    }

    public Properties getPasswords()
    {
        return passwords;
    }

    public void setPasswords(Properties passwords)
    {
        this.passwords = passwords;
    }
    
    
}



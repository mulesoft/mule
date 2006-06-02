/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.acegi;

import net.sf.acegisecurity.context.ContextInvalidException;
import net.sf.acegisecurity.context.SecureContext;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;

/**
 * <code>AcegiSecurityContext</code> is a UMOSecurityContext wrapper used to
 * interface with an acegi Secure context
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class AcegiSecurityContext implements UMOSecurityContext
{
    private SecureContext delegate;
    private AcegiAuthenticationAdapter authentication;

    public AcegiSecurityContext(SecureContext delegate)
    {
        this.delegate = delegate;
    }

    public void setAuthentication(UMOAuthentication authentication)
    {
        this.authentication = ((AcegiAuthenticationAdapter) authentication);
        delegate.setAuthentication(this.authentication.getDelegate());
    }

    public UMOAuthentication getAuthentication()
    {
        return authentication;
    }

    public void validate() throws ContextInvalidException
    {
        delegate.validate();
    }
}

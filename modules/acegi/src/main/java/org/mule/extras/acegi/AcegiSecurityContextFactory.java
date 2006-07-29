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
package org.mule.extras.acegi;

import net.sf.acegisecurity.context.SecureContext;
import net.sf.acegisecurity.context.SecureContextImpl;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;

/**
 * <code>AcegiSecurityContextFactory</code> creates an Acegisecuritycontext
 * for the a UMOAuthentication object
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AcegiSecurityContextFactory implements UMOSecurityContextFactory
{
    public UMOSecurityContext create(UMOAuthentication authentication)
    {
        SecureContext context = new SecureContextImpl();
        context.setAuthentication(((AcegiAuthenticationAdapter) authentication).getDelegate());
        return new AcegiSecurityContext(context);
    }
}

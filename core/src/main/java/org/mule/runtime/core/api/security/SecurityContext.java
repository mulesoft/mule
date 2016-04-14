/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import java.io.Serializable;

/**
 * <code>SecurityContext</code> holds security information and is associated
 * with the MuleSession.
 * 
 * @see org.mule.api.MuleSession
 */

public interface SecurityContext extends Serializable
{
    void setAuthentication(Authentication authentication);

    Authentication getAuthentication();
}

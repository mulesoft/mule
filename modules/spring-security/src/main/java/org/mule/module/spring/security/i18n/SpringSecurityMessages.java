/*
 * $Id: AcegiMessages.java 11456 2008-03-20 17:52:27Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.security.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class SpringSecurityMessages extends MessageFactory
{
    private static final SpringSecurityMessages factory = new SpringSecurityMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("spring-security");

    public static Message basicFilterCannotHandleHeader(String header)
    {
        return factory.createMessage(BUNDLE_PATH, 1, header);
    }

    public static Message authRealmMustBeSetOnFilter()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }
}



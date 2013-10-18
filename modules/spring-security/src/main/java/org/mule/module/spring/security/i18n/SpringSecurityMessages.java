/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

    public static Message noGrantedAuthority(String authority)
    {
        return factory.createMessage(BUNDLE_PATH, 3, authority);
    }

    public static Message springAuthenticationRequired()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }
}



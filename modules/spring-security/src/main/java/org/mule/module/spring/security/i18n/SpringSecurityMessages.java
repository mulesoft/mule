/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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



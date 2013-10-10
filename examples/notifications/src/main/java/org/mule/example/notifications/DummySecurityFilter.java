/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.notifications;

import org.mule.api.MuleEvent;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.AbstractAuthenticationFilter;
import org.mule.transformer.types.DataTypeFactory;

import java.util.Map;

/**
 * TODO
 */
public class DummySecurityFilter extends AbstractAuthenticationFilter
{
    @Override
    public void authenticate(MuleEvent event) throws UnauthorisedException
    {
        try
        {
            Map<?, ?> payload = event.getMessage().getPayload(DataTypeFactory.create(Map.class));
            String user = (String) payload.get("user");
            if (user == null)
            {
                throw new UnauthorisedException(CoreMessages.authNoCredentials(), event);
            }
            if ("anonymous".equals(user))
            {
                throw new UnauthorisedException(CoreMessages.authFailedForUser("anonymous"), event);
            }
        }
        catch (TransformerException te)
        {
            throw new UnauthorisedException(CoreMessages.transformFailed("Object", "Map"), event, te);
        }
    }
}

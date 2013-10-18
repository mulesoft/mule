/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

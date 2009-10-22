/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.routing;

import org.mule.api.MuleException;
import org.mule.api.RouterAnnotationParser;
import org.mule.api.routing.Router;
import org.mule.config.annotations.routing.Idempotent;
import org.mule.routing.inbound.IdempotentReceiver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * Parses an {@link org.mule.config.annotations.routing.Idempotent} annotation into a Mule {@link org.mule.routing.inbound.IdempotentReceiver}
 * and registers it with the service it is configured on.
 */
public class IdempotentRouterParser implements RouterAnnotationParser
{
    public Router parseRouter(Annotation annotation) throws MuleException
    {
        Idempotent router = (Idempotent) annotation;

      //  if (router.type() == Idempotent.Type.ID)
       // {
            return new IdempotentReceiver();
      //  }
//        else //if (router.type() == Idempotent.Type.HASH)
//        {
//            return new IdempotentSecureHashReceiver();
//        }
    }

    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        return annotation instanceof Idempotent;
    }
}
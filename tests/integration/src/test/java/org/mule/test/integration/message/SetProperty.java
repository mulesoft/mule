/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.message;

import org.mule.RequestContext;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class SetProperty extends AbstractTransformer
{
    @Override
    protected Object doTransform(Object obj, String encoding) throws TransformerException
    {
        RequestContext.getEventContext().getSession().setProperty("foo", "bar");
        return obj;
    }
}



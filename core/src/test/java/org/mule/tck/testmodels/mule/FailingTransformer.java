/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractTransformer;

public class FailingTransformer extends AbstractTransformer
{
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        throw new TransformerException(MessageFactory.createStaticMessage("Failure"));
    }
}

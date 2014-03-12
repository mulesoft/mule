/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractTransformer;

public class AlwaysRaiseExceptionTransformer extends AbstractTransformer
{

    @Override
    protected Object doTransform(Object src, String enc) throws TransformerException
    {
        throw new TransformerException(MessageFactory.createStaticMessage("Ad hoc message exception"));
    }
}

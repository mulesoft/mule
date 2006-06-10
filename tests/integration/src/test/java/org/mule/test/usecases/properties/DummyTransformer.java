/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.usecases.properties;

import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

public class DummyTransformer extends AbstractEventAwareTransformer
{
    private static final long serialVersionUID = 8536127232968197199L;

    public Object transform(Object src, String encoding, UMOEventContext context)
            throws TransformerException
    {
        System.out.println("org.mule.test.usecases.props.DummyTransformer");

        PropsComponent.assertEquals("param1", context.getMessage().getProperty("stringParam"));
        PropsComponent.assertEquals(PropsComponent.testObjectProperty,
                context.getMessage().getProperty("objectParam"));

        System.out.println("org.mule.test.usecases.props.DummyTransformer done.");
        return src;
    }
}

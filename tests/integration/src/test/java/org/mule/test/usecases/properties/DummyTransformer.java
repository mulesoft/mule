/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.properties;

import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

public class DummyTransformer extends AbstractEventAwareTransformer
{

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        logger.debug("transform() starting.");

        PropsComponent.assertEquals("param1", context.getMessage().getProperty("stringParam"));
        PropsComponent.assertEquals(PropsComponent.testObjectProperty, context.getMessage().getProperty(
            "objectParam"));

        logger.debug("transform() done.");
        return src;
    }

}

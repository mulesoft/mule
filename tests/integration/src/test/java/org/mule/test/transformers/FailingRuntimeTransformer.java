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
package org.mule.test.transformers;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

public class FailingRuntimeTransformer extends AbstractTransformer {
    private static final long serialVersionUID = -9087617311285981031L;

    protected Object doTransform(Object src, String encoding) throws TransformerException {
        throw new RuntimeException("test");
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.editors;

import org.mule.RegistryContext;

import java.beans.PropertyEditorSupport;
import java.util.StringTokenizer;

/**
 * Translates a transformer name property into a transformer instance. If more than one transformer
 * name is supplied, each will be resolved and chained together.
 */
public class TransformerPropertyEditor extends PropertyEditorSupport
{
    public void setAsText(String text) {

        StringTokenizer st = new StringTokenizer(text);
        TransformerChain chain = new TransformerChain();

        while (st.hasMoreTokens())
        {
            String name = st.nextToken().trim();
            chain.addTransformer(RegistryContext.getRegistry().lookupTransformer(name));
        }
        setValue(chain);
    }

}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.editors;

import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.umo.transformer.UMOTransformer;

import java.beans.PropertyEditorSupport;
import java.util.StringTokenizer;

/**
 * Translates a transformer name property into a transformer instance. If more than one transformer
 * name is supplied, ech will be resolved and chained together.
 */
public class TransformerPropertyEditor extends PropertyEditorSupport
{
    public void setAsText(String text) {

        StringTokenizer st = new StringTokenizer(text, " ");
        UMOTransformer currentTrans = null;
        UMOTransformer returnTrans = null;

        while (st.hasMoreTokens())
        {
            String name = st.nextToken().trim();
            UMOTransformer tempTrans = RegistryContext.getRegistry().lookupTransformer(name);

            if (tempTrans == null)
            {
                throw new IllegalArgumentException(CoreMessages.objectNotFound(name).getMessage());
            }

            if (currentTrans == null)
            {
                currentTrans = tempTrans;
                returnTrans = tempTrans;
            }
            else
            {
                currentTrans.setNextTransformer(tempTrans);
                currentTrans = tempTrans;
            }
        }
        setValue(returnTrans);
    }
}

/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.MuleException;
import org.mule.util.MuleObjectHelper;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.beans.PropertyEditorSupport;

/**
 * <code>TransformerEditor</code> is used to convert Transformer names
 * into transformer Objects
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TransformerEditor extends PropertyEditorSupport
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TransformerEditor.class);

    public void setAsText(String text) {
        try
        {
            setValue(MuleObjectHelper.getTransformer(text, (text.indexOf(",") > -1 ? "," : " ")));
        } catch (MuleException e)
        {
            logger.error(e.getMessage(), e);
        }
    }
}

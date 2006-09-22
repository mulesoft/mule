/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.util.MuleObjectHelper;

import java.beans.PropertyEditorSupport;

/**
 * <code>TransformerEditor</code> is used to convert Transformer names into
 * transformer Objects
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransformerEditor extends PropertyEditorSupport
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TransformerEditor.class);

    public void setAsText(String text)
    {
        try {
            setValue(MuleObjectHelper.getTransformer(text, (text.indexOf(",") > -1 ? "," : " ")));
        } catch (MuleException e) {
            logger.error(e.getMessage(), e);
        }
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.hello;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>NameStringToChatString</code> cnverts from a NameString object to a ChatString
 * object
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NameStringToChatString extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6223698853238962982L;

    public NameStringToChatString()
    {
        super();
        this.registerSourceType(NameString.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        ChatString string = new ChatString();
        NameString ns = (NameString)src;
        string.append(ns.getGreeting());
        String name = ns.getName();
        // strip out the new line from the console
        name = name.replaceAll("\n", "");
        string.append(name);
        return string;
    }
}

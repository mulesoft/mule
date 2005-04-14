/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.samples.hello;

import org.mule.transformers.DefaultTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>NameStringToChatString</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NameStringToChatString extends DefaultTransformer
{
    /**
     *
     */
    public NameStringToChatString()
    {
        super();
        this.registerSourceType(NameString.class);
    }

    /* (non-Javadoc)
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src) throws TransformerException
    {
        ChatString string = new ChatString();
        NameString ns = (NameString) src;
        string.append(ns.getGreeting());
        String name = ns.getName();
        //strip out the new line from the console
        name = name.replaceAll("\n", "");
        string.append(name);
        return string;
    }
}

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
 *
 */
 
package org.mule.samples.hello;

/**
 *  <code>ChitChatter</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ChitChatter
{
    String chitchat = ", How are you?";
   
    /**
     * 
     */
    public void chat(ChatString string)
    {
       string.append(chitchat);
    }

}

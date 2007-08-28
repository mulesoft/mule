/*
 * $Id: ChitChatter.java 3650 2006-10-24 10:26:34 +0000 (Tue, 24 Oct 2006) holger $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.hello;

/**
 * <code>ChitChatter</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 3650 $
 */
public class ChitChatter
{
    String chitchat = ", How are you?";

    public void chat(ChatString string)
    {
        string.append(chitchat);
    }

}

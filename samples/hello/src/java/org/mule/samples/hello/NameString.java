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

import java.io.Serializable;

/**
 *  <code>NameString</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NameString implements Serializable
{
    private String name;
    private String greeting;
    /**
     * 
     */
    public NameString(String name)
    {
        this.name = name;
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }
    /**
     * @return Returns the greeting.
     */
    public String getGreeting()
    {
        return greeting;
    }

    /**
     * @param greeting The greeting to set.
     */
    public void setGreeting(String greeting)
    {
        this.greeting = greeting;
    }

}

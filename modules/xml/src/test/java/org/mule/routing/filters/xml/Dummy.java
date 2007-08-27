/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters.xml;

public class Dummy
{

    private int id;
    private String content;

    public Dummy()
    {
        super();
    }

    /**
     * @return Returns the content.
     */
    public String getContent()
    {
        return content;
    }

    /**
     * @param content The content to set.
     */
    public void setContent(String content)
    {
        this.content = content;
    }

    /**
     * @return Returns the id.
     */
    public int getId()
    {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(int id)
    {
        this.id = id;
    }

}

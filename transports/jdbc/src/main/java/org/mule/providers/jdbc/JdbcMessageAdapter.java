/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.mule.providers.AbstractMessageAdapter;

import java.util.Map;

/**
 * TODO
 */
public class JdbcMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6770314376258549559L;

    private Map map;

    public JdbcMessageAdapter(Object obj)
    {
        this.map = (Map)obj;
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        return map.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return map.toString().getBytes();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        return map;
    }

}

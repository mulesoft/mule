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
 */
package org.mule.providers.jdbc;

import java.util.Map;

import org.mule.providers.AbstractMessageAdapter;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcMessageAdapter extends AbstractMessageAdapter
{

    private Map map;

    public JdbcMessageAdapter(Object obj)
    {
        this.map = (Map) obj;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageAdapter#getPayloadAsString()
     */
    public String getPayloadAsString() throws Exception
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

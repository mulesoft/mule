/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

public class FileMessageRequesterTestCase extends AbstractMuleTestCase
{
    
    private static final String CONNECTOR_MOVE_DIR = "connector/moveto";
    private static final String ENDPOINT_MOVE_DIR = "endpoint/moveto";
    private static final String CONNECTOR_MOVE_TO_PATTERN = "#connector";
    private static final String ENDPOINT_MOVE_TO_PATTERN = "#endpoint";

    private FileConnector connector;
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        connector = new FileConnector();
        connector.setMoveToDirectory(CONNECTOR_MOVE_DIR);        
        connector.setMoveToPattern(CONNECTOR_MOVE_TO_PATTERN);
    }

    public void testMoveDirectoryFromConnector() throws Exception
    {
        FileMessageRequester requester = new FileMessageRequester(createEndpoint());        
        assertEquals(CONNECTOR_MOVE_DIR, requester.getMoveDirectory());
    }
    
    public void testMoveDirectoryFromEndpoint() throws Exception
    {
        InboundEndpoint endpoint = createEndpoint(FileConnector.PROPERTY_MOVE_TO_DIRECTORY, 
            ENDPOINT_MOVE_DIR);
        FileMessageRequester requester = new FileMessageRequester(endpoint);
        assertEquals(ENDPOINT_MOVE_DIR, requester.getMoveDirectory());
    }
    
    public void testMoveToPatternFromConnector()
    {
        FileMessageRequester requester = new FileMessageRequester(createEndpoint());
        assertEquals(CONNECTOR_MOVE_TO_PATTERN, requester.getMoveToPattern());
    }
    
    public void testMoveToPatternFromEndpoint()
    {
        InboundEndpoint endpoint = createEndpoint(FileConnector.PROPERTY_MOVE_TO_PATTERN,
            ENDPOINT_MOVE_TO_PATTERN);
        FileMessageRequester requester = new FileMessageRequester(endpoint);
        assertEquals(ENDPOINT_MOVE_TO_PATTERN, requester.getMoveToPattern());
    }
    
    private InboundEndpoint createEndpoint()
    {
        return createEndpoint(null, null);
    }

    private InboundEndpoint createEndpoint(Object key, Object value)
    {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        if (key != null)
        {
            properties.put(key, value);
        }
        
        return new DefaultInboundEndpoint(connector, null, null, null, null, 
            properties, null, null, false, null, false, false, 42, null, null, null,
            muleContext, null);
    }

}

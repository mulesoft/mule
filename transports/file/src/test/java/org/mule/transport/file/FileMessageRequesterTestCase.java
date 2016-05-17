/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.MessageExchangePattern;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileMessageRequesterTestCase extends AbstractMuleContextTestCase
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
        
        connector = new FileConnector(muleContext);
        connector.setMoveToDirectory(CONNECTOR_MOVE_DIR);        
        connector.setMoveToPattern(CONNECTOR_MOVE_TO_PATTERN);
    }

    @Test
    public void testMoveDirectoryFromConnector() throws Exception
    {
        FileMessageRequester requester = new FileMessageRequester(createEndpoint());        
        assertEquals(CONNECTOR_MOVE_DIR, requester.getMoveDirectory());
    }
    
    @Test
    public void testMoveDirectoryFromEndpoint() throws Exception
    {
        InboundEndpoint endpoint = createEndpoint(FileConnector.PROPERTY_MOVE_TO_DIRECTORY, 
            ENDPOINT_MOVE_DIR);
        FileMessageRequester requester = new FileMessageRequester(endpoint);
        assertEquals(ENDPOINT_MOVE_DIR, requester.getMoveDirectory());
    }
    
    @Test
    public void testMoveToPatternFromConnector() throws Exception
    {
        FileMessageRequester requester = new FileMessageRequester(createEndpoint());
        assertEquals(CONNECTOR_MOVE_TO_PATTERN, requester.getMoveToPattern());
    }
    
    @Test
    public void testMoveToPatternFromEndpoint() throws Exception
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
        
        return new DefaultInboundEndpoint(connector, null, null, properties, null, 
            false, MessageExchangePattern.ONE_WAY, 42, null, null, null, 
            muleContext, null, null, null, null, null, false, null);
    }
}

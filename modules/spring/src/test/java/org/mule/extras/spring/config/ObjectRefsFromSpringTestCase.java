/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

        protected String getConfigResources() {
            return "test-refs-from-spring.xml";
        }

    public void testObjectCreation() throws Exception
    {
        UMOEndpoint ep = MuleManager.getInstance().lookupEndpoint("foo");
        assertNotNull(ep);
        assertEquals("testConnector", ep.getConnector().getName());
        assertTrue(((AbstractConnector)ep.getConnector()).getConnectionStrategy() instanceof SimpleRetryConnectionStrategy);
        assertTrue(ep.getConnector().getExceptionListener() instanceof TestExceptionStrategy);

        assertNotNull(ep.getTransformer());
        assertEquals("testTransformer", ep.getTransformer().getName());
        assertTrue(ep.getTransformer() instanceof TestCompressionTransformer);
        assertEquals(12, ((TestCompressionTransformer)ep.getTransformer()).getBeanProperty2());
        
    }
}

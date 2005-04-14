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
package org.mule.test.config;

import org.mule.components.simple.EchoComponent;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ConnectorCreationTestCase extends NamedTestCase {
    public void testAlwaysCreateUsingParamString() throws Exception {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        builder.registerEndpoint("test://inbound?createConnector=ALWAYS", "in", true);
        builder.registerEndpoint("test://outbound?createConnector=ALWAYS", "out", false);
        UMOComponent c = builder.registerComponent(EchoComponent.class.getName(), "echo", "in", "out", null);
        assertTrue(!c.getDescriptor().getInboundEndpoint().getConnector().equals(c.getDescriptor().getOutboundEndpoint().getConnector()));
    }

//    public void testAlwaysCreateUsingProperties() throws Exception {
//        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
//        Map props = new HashMap();
//        props.put("createConnector", "ALWAYS");
//
//        builder.registerEndpoint("test://inbound", "in", true, props);
//        builder.registerEndpoint("test://outbound", "out", false, props);
//        UMOComponent c = builder.registerComponent(EchoComponent.class.getName(), "echo", "in", "out", null);
//        assertTrue(!c.getDescriptor().getInboundEndpoint().getConnector().equals(c.getDescriptor().getOutboundEndpoint().getConnector()));
//    }


    public void testCreateOnce() throws Exception {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        builder.registerEndpoint("test://inbound", "in", true);
        builder.registerEndpoint("test://outbound", "out", false);
        UMOComponent c = builder.registerComponent(EchoComponent.class.getName(), "echo", "in", "out", null);
        assertEquals(c.getDescriptor().getInboundEndpoint().getConnector(), c.getDescriptor().getOutboundEndpoint().getConnector());
    }

    public void testCreateNeverUsingParamString() throws Exception {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        try {
            builder.registerEndpoint("test://inbound?createConnector=NEVER", "in", true);
            fail("Should fail as there is no existing test connector");
        } catch (UMOException e) {
        }
    }

//    public void testCreateNeverUsingProperties() throws Exception {
//        Map props = new HashMap();
//        props.put("createConnector", "NEVER");
//        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
//        try {
//            builder.registerEndpoint("test://inbound", "in", true, props);
//            fail("Should fail as there is no existing test connector");
//        } catch (UMOException e) {
//
//        }
//    }
}

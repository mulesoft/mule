/*
 * $Id$
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
package org.mule.impl;

import org.mule.MuleManager;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.model.ComponentUtil;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MuleComponentTestCase extends AbstractMuleTestCase
{
    protected void doSetUp() throws Exception {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        builder.createStartedManager(true, "");
        builder.registerComponent(Orange.class.getName(), "orangeComponent", "test://in", "test://out", null);
    }

    public void testSendToPausedComponent() throws Exception
    {
        UMOSession session = MuleManager.getInstance().getModel().getComponentSession("orangeComponent");
        final UMOComponent comp = session.getComponent();
        assertTrue(comp.isStarted());
        ComponentUtil.pause(comp);
        new Thread(new Runnable() {
            public void run()
            {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // ignore
                }
                try {
                    ComponentUtil.resume(comp);
                } catch (UMOException e) {
                    fail(e.getMessage());
                }
            }
        }).start();
        long t0 = System.currentTimeMillis();
        comp.sendEvent(getTestEvent("hello"));
        long t1 = System.currentTimeMillis();
        assertTrue(t1 - t0 > 1000);
    }

    public void testSendToStoppedComponent() throws Exception
    {
        MuleDescriptor descriptor = getTestDescriptor("myComponent", "org.mule.components.simple.EchoComponent");
        UMOComponent comp = getTestComponent(descriptor);
        assertTrue(!comp.isStarted());

        try {
            comp.dispatchEvent(getTestEvent("hello"));
            fail();
        } catch (ComponentException e) {
            // expected
        }

        try {
            comp.sendEvent(getTestEvent("hello"));
            fail();
        } catch (ComponentException e) {
            // expected
        }
    }
}

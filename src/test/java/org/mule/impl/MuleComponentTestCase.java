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
package org.mule.impl;

import org.mule.MuleManager;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOSession;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MuleComponentTestCase extends FunctionalTestCase
{

    protected String getConfigResources() {
        return "test-xml-mule-config.xml";
    }

    public void testSendToPausedComponent() throws Exception
    {
        UMOSession session = MuleManager.getInstance().getModel().getComponentSession("orangeComponent");
        final MuleComponent comp = (MuleComponent) session.getComponent();
        assertFalse(comp.isStopped());
        comp.pause();
        new Thread(new Runnable() {
            public void run()
            {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                comp.resume();
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
        MuleComponent comp = getTestComponent(descriptor);
        assertTrue(comp.isStopped());
        try {
            comp.dispatchEvent(getTestEvent("hello"));
            fail();
        } catch (ComponentException e) {
        }
        try {
            comp.sendEvent(getTestEvent("hello"));
            fail();
        } catch (ComponentException e) {
        }
    }
}

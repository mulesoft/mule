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

package org.mule.impl.model.seda;

import org.mule.MuleRuntimeException;
import org.mule.impl.MuleDescriptor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public class SedaComponentTestCase extends AbstractMuleTestCase {

    public void testSpiWorkThrowableHandling() throws Exception {
        try {
            // getTestComponent() currently already returns a SedaComponent, but
            // here we are safe-guarding for any future changes
            MuleDescriptor descriptor = MuleTestUtils.getTestDescriptor("test", "java.lang.Object");
            SedaComponent component = new SedaComponent(descriptor, new SedaModel());

            component.handleWorkException(getTestWorkEvent(), "workRejected");
        } catch (MuleRuntimeException mrex) {
            assertNotNull(mrex);
            assertTrue(mrex.getCause().getClass() == Throwable.class);
            assertEquals("testThrowable", mrex.getCause().getMessage());
        }
    }

    private WorkEvent getTestWorkEvent() {
        return new WorkEvent(this, // source
                                        WorkEvent.WORK_REJECTED,
                                        getTestWork(),
                                        new WorkException(new Throwable("testThrowable")));
    }

    private Work getTestWork() {
        return new Work() {
            public void release() {
                // noop
            }

            public void run() {
                // noop
            }
        };
    }
}

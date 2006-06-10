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
package org.mule.umo;


/**
 * Adds the ability to pause/resume event processing to the <code>UMOComponent</code>
 * interface.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */

public interface UMOAsynchronousComponent extends UMOComponent {
    /**
     * Pauses event processing for theComponent. Unlike stop(), a paused
     * component will still consume messages from the underlying transport, but
     * those messages will be queued until the component is resumed.
     *
     * In order to persist these queued messages you can set the
     * 'recoverableMode' property on the Mule configuration to true.
     * This causes all internal queues to store their state.
     *
     * @throws UMOException if the component failed to pause.
     * @see org.mule.config.MuleConfiguration
     */
    void pause() throws UMOException;

    /**
     * Resumes the Component that has been paused. If the component is not
     * paused nothing is executed.
     *
     * @throws UMOException if the component failed to resume
     */
    void resume() throws UMOException;

    boolean isPaused();
}

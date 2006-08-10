/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Adds the ability to pause/resume event processing to <code>AbstractComponent</code>.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public abstract class AbstractAsynchronousComponent
                        extends AbstractComponent implements UMOAsynchronousComponent {

    /**
     * Determines if the component has been paused
     */
    protected WaitableBoolean paused = new WaitableBoolean(false);

    /**
     * Default constructor
     */
    public AbstractAsynchronousComponent(MuleDescriptor descriptor, UMOModel model) {
        super(descriptor, model);
    }

    public final void pause() {
        doPause();
        paused.set(true);
        fireComponentNotification(ComponentNotification.COMPONENT_PAUSED);
    }

    public final void resume() {
        doResume();
        paused.set(false);
        fireComponentNotification(ComponentNotification.COMPONENT_RESUMED);
    }

    public boolean isPaused() {
        return paused.get();
    }

    protected void doPause() {
        // template method
    }

    protected void doResume() {
        // template method
    }

    public UMOMessage sendEvent(UMOEvent event) throws UMOException {

        if (logger.isDebugEnabled() && paused.get()) {
            logger.debug("Component: " + descriptor.getName() + " is paused. Blocking call until resume is called");
        }
        try {
            paused.whenFalse(null);
        } catch (InterruptedException e) {
            throw new ComponentException(event.getMessage(), this, e);
        }

        return super.sendEvent(event);
    }
}

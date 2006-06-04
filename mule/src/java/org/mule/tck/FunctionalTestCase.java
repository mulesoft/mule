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
 */
package org.mule.tck;

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.manager.DefaultWorkListener;

import javax.resource.spi.work.WorkEvent;

/**
 * Is a base tast case for tests that initialise Mule using a configuration file
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class FunctionalTestCase extends AbstractMuleTestCase {

    protected final void doSetUp() throws Exception {
        doPreFunctionalSetUp();
        //Should we set up te manager for every method?
        if (!getTestInfo().isDisposeManagerPerSuite()) {
            setupManager();
        }
        doPostFunctionalSetUp();
    }

    protected void suitePreSetUp() throws Exception {
        if(getTestInfo().isDisposeManagerPerSuite()) {
            setupManager();
        }
    }

    private void setupManager() throws Exception {
        MuleManager.getConfiguration().setWorkListener(new TestingWorkListener());
        ConfigurationBuilder builder = getBuilder();
        builder.configure(getConfigResources());
    }

    protected final void doTearDown() throws Exception {
        doFunctionalTearDown();
    }

    protected ConfigurationBuilder getBuilder() throws Exception {
        return new MuleXmlConfigurationBuilder();
    }

    protected void doPreFunctionalSetUp() throws Exception {
        // template method
    }

    protected void doPostFunctionalSetUp() throws Exception {
        // template method
    }

    protected void doFunctionalTearDown() throws Exception {
        // template method
    }

    protected abstract String getConfigResources();

    public class TestingWorkListener extends DefaultWorkListener {
        protected void handleWorkException(WorkEvent event, String type) {
            super.handleWorkException(event, type);
            if(event.getException()!=null) {
                Throwable t = event.getException().getCause();
                if(t!=null) {
                    
                    if(t instanceof Error) {
                        throw (Error)t;
                    } else if(t instanceof RuntimeException) {
                        throw (RuntimeException)t;
                    }
                }

            }
        }
    }

}

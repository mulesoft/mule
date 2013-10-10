/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.module.launcher.application.Application;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DefaultMuleDeployerTestCase extends AbstractMuleTestCase
{

    @Test
    public void disposesAppOnDeployFailure() throws Exception
    {
        DefaultMuleDeployer deployer = new DefaultMuleDeployer();
        Application app = mock(Application.class);
        doThrow(new IllegalStateException()).when(app).init();

        try
        {
            deployer.deploy(app);
            fail("Deployment is supposed to fail");
        }
        catch (DeploymentException expected)
        {
        }

        verify(app, times(1)).dispose();
    }
}

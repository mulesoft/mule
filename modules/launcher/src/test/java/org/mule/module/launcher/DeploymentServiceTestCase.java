package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 *
 */
public class DeploymentServiceTestCase extends AbstractMuleTestCase
{

    protected File muleHome;
    protected File appsDir;
    protected DeploymentService deploymentService;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // set up some mule home structure
        final String tmpDir = System.getProperty("java.io.tmpdir");
        muleHome = new File(tmpDir, getClass().getSimpleName() + System.currentTimeMillis());
        appsDir = new File(muleHome, "apps");
        appsDir.mkdirs();
        System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

        new File(muleHome, "lib/shared/default").mkdirs();

        deploymentService = new DeploymentService();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        FileUtils.deleteTree(muleHome);
        if (deploymentService != null)
        {
            deploymentService.stop();
        }
        super.doTearDown();
    }

    public void testDeployment() throws Exception
    {
        Latch latch = new Latch();
        final TestDeployer deployer = new TestDeployer(latch);
        deploymentService.setDeployer(deployer);

        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found", url);
        FileUtils.copyFileToDirectory(new File(url.getFile()), appsDir);

        deploymentService.start();

        assertTrue("Deployer never invoked", latch.await(5000, TimeUnit.MILLISECONDS));
    }

    private class TestDeployer implements MuleDeployer
    {
        MuleDeployer delegate = new DefaultMuleDeployer();
        Latch latch;

        private TestDeployer(Latch latch)
        {
            this.latch = latch;
        }

        public void deploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.deploy");
            latch.release();
            delegate.deploy(app);
        }

        public void undeploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.undeploy");
            delegate.undeploy(app);
        }

        public Application installFromAppDir(String packedMuleAppFileName) throws IOException
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFromAppDir");
            return delegate.installFromAppDir(packedMuleAppFileName);
        }

        public Application installFrom(URL url) throws IOException
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFrom");
            return delegate.installFrom(url);
        }
    }
}

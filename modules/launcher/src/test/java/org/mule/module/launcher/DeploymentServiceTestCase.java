package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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
        final String tmpDir = System.getProperty("java.io.tmpdir");
        muleHome = new File(tmpDir, getClass().getSimpleName() + System.currentTimeMillis());
        appsDir = new File(muleHome, "apps");
        appsDir.mkdirs();
        System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

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
        final TestDeployer deployer = new TestDeployer();
        deploymentService.setDeployer(deployer);
        deploymentService.start();
    }

    private class TestDeployer implements MuleDeployer
    {

        public void deploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.deploy");
        }

        public void undeploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.undeploy");
        }

        public Application installFromAppDir(String packedMuleAppFileName) throws IOException
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFromAppDir");
            return null;
        }

        public Application installFrom(URL url) throws IOException
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFrom");
            return null;
        }
    }
}

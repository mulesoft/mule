package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import static org.junit.Assert.assertArrayEquals;

/**
 *
 */
public class DeploymentServiceTestCase extends AbstractMuleTestCase
{

    protected static final int LATCH_TIMEOUT = 10000;
    protected static final String[] NONE = new String[0];

    protected File muleHome;
    protected File appsDir;
    protected DeploymentService deploymentService;
    // these latches are re-created during the test, thus need to be declared volatile
    protected volatile Latch deployLatch;
    protected volatile Latch undeployLatch;

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
        deploymentService.setDeployer(new TestDeployer());
        deployLatch = new Latch();
        undeployLatch = new Latch();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        //FileUtils.deleteTree(muleHome);
        if (deploymentService != null)
        {
            deploymentService.stop();
        }
        super.doTearDown();
    }

    public void testDeployZipOnStartup() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found", url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        assertAppsDir(NONE, new String[] {"dummy-app"});
    }

    public void testUpdateAppViaZip() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found", url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"dummy-app"});
        // set up a new deployment latch (can't reuse the old one)
        deployLatch = new Latch();
        addAppArchive(url);
        assertTrue("Undeploy never invoked", undeployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"dummy-app"});
    }

    private void assertAppsDir(String[] expectedZips, String[] expectedApps)
    {
        final String[] actualZips = appsDir.list(new SuffixFileFilter(".zip"));
        assertArrayEquals("Invalid Mule application archives set", expectedZips, actualZips);
        final String[] actualApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
        assertArrayEquals("Invalid Mule exploded applications set", expectedApps, actualApps);
    }

    private void addAppArchive(URL url) throws IOException
    {
        // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
        final String tempFileName = new File(url.getFile() + ".part").getName();
        final File tempFile = new File(appsDir, tempFileName);
        FileUtils.copyURLToFile(url, tempFile);
        tempFile.renameTo(new File(StringUtils.removeEnd(tempFile.getAbsolutePath(), ".part")));
    }


    private class TestDeployer implements MuleDeployer
    {
        MuleDeployer delegate = new DefaultMuleDeployer();

        public void deploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.deploy");
            delegate.deploy(app);
            deployLatch.release();
        }

        public void undeploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.undeploy");
            delegate.undeploy(app);
            undeployLatch.release();
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

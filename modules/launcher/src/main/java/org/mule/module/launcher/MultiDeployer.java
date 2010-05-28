package org.mule.module.launcher;

import org.mule.api.MuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Deploys multiple applications.
 */
public class MultiDeployer implements Deployer
{

    private List<Deployer> deployers = new ArrayList<Deployer>();

    public void install() throws InstallException
    {
        // no-op, apps handled in start()
    }

    public void init()
    {
        // no-op, apps handled in start()
    }

    public void start() throws DeploymentStartException
    {
        for (Deployer deployer : deployers)
        {
            try
            {
                deployer.install();
                deployer.init();
                deployer.start();
            }
            catch (DeploymentException e)
            {
                // TODO MultiXXXException to report multiple failures
                e.printStackTrace();
            }
        }
    }

    public void stop()
    {
        for (Deployer deployer : deployers)
        {
            try
            {
                deployer.stop();
            }
            catch (DeploymentException e)
            {
                // TODO MultiXXXException to report multiple failures
                e.printStackTrace();
            }
        }
    }

    public void dispose()
    {
        for (Deployer deployer : deployers)
        {
            try
            {
                deployer.dispose();
            }
            catch (DeploymentException e)
            {
                // TODO MultiXXXException to report multiple failures
                e.printStackTrace();
            }
        }
    }

    public void redeploy()
    {
        for (Deployer deployer : deployers)
        {
            try
            {
                deployer.redeploy();
            }
            catch (DeploymentException e)
            {
                // TODO MultiXXXException to report multiple failures
                e.printStackTrace();
            }
        }
    }

    public void setMetaData(Object metaData)
    {
        throw new UnsupportedOperationException("Ambiguous call, set metadata on a specific deployer instead.");
    }

    public Object getMetaData()
    {
        throw new UnsupportedOperationException("Ambiguous call, obtain metadata from a specific deployer directly.");
    }

    public MuleContext getMuleContext()
    {
        throw new UnsupportedOperationException("getMuleContext");
    }

    public ClassLoader getDeploymentClassLoader()
    {
        throw new UnsupportedOperationException("getDeploymentClassLoader");
    }

    public List<Deployer> getDeployers()
    {
        return deployers;
    }
}

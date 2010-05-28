package org.mule.module.launcher;

import org.mule.api.MuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Deploys multiple applications.
 */
public class MultiApplication implements Application
{

    private List<Application> applications = new ArrayList<Application>();

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
        for (Application application : applications)
        {
            try
            {
                application.install();
                application.init();
                application.start();
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
        for (Application application : applications)
        {
            try
            {
                application.stop();
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
        for (Application application : applications)
        {
            try
            {
                application.dispose();
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
        for (Application application : applications)
        {
            try
            {
                application.redeploy();
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

    public List<Application> getDeployers()
    {
        return applications;
    }
}

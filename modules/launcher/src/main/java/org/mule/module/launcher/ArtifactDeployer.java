/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.util.SplashScreen.miniSplash;

import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.artifact.ArtifactFactory;
import org.mule.module.launcher.util.ObservableList;
import org.mule.util.CollectionUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Deployer of an artifact within mule container.
 * - Keeps track of deployed artifacts
 * - Avoid already deployed artifacts to be redeployed
 * - Deploys, undeploys, redeploys packaged and exploded artifacts
 */
public class ArtifactDeployer<T extends Artifact>
{

    public static final String ARTIFACT_NAME_PROPERTY = "artifactName";
    protected transient final Log logger = LogFactory.getLog(getClass());
    public static final String ZIP_FILE_SUFFIX = ".zip";
    public static final String ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS = "Another deployment operation is in progress";
    public static final String INSTALL_OPERATION_HAS_BEEN_INTERRUPTED = "Install operation has been interrupted";

    private Map<String, ZombieFile> artifactZombieMap = new HashMap<String, ZombieFile>();
    private File artifactDir;
    private ObservableList<T> artifacts;
    private DeploymentListener deploymentListener;
    private ReentrantLock deploymentInProgressLcok;
    protected MuleDeployer<T> deployer;
    protected ArtifactFactory<T> artifactFactory;

    public ArtifactDeployer(DeploymentListener deploymentListener, MuleDeployer deployer, ArtifactFactory artifactFactory, ObservableList<T> artifacts, ReentrantLock lock)
    {
        this.deploymentListener = deploymentListener;
        this.deployer = deployer;
        this.artifactFactory = artifactFactory;
        this.artifacts = artifacts;
        this.deploymentInProgressLcok = lock;
        this.artifactDir = artifactFactory.getArtifactDir();
    }


    public void deployPackagedArtifact(String zip) throws Exception
    {
        URL url;
        File artifactZip;

        final String artifactName = StringUtils.removeEnd(zip, ZIP_FILE_SUFFIX);

        artifactZip = new File(artifactDir, zip);
        url = artifactZip.toURI().toURL();

        ZombieFile zombieFile = artifactZombieMap.get(artifactName);
        if (zombieFile != null)
        {
            if (zombieFile.isFor(url) && !zombieFile.updatedZombieApp())
            {
                // Skips the file because it was already deployed with failure
                return;
            }
        }

        // check if this artifact is running first, undeploy it then
        T artifact = (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
        if (artifact != null)
        {
            undeploy(artifactName);
        }

        deploy(url);
    }

    public void deployExplodedArtifact(String artifactName) throws DeploymentException
    {
        @SuppressWarnings("rawtypes")
        Collection<String> deployedAppNames = CollectionUtils.collect(artifacts, new BeanToPropertyValueTransformer(ARTIFACT_NAME_PROPERTY));

        if (deployedAppNames.contains(artifactName) && (!artifactZombieMap.containsKey(artifactName)))
        {
            return;
        }

        ZombieFile zombieFile = artifactZombieMap.get(artifactName);

        if ((zombieFile != null) && (!zombieFile.updatedZombieApp()))
        {
            return;
        }

        deployExplodedApp(artifactName);
    }

    public Map<String, ZombieFile> getArtifactZombieMap()
    {
        return artifactZombieMap;
    }

    private void deployExplodedApp(String addedApp) throws DeploymentException
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== New Exploded Artifact: " + addedApp);
        }

        T artifact;
        try
        {
            artifact = artifactFactory.createArtifact(addedApp);

            // add to the list of known artifacts first to avoid deployment loop on failure
            onArtifactInstalled(artifact);
        }
        catch (Throwable t)
        {
            final File artifactDir1 = artifactDir;
            File artifactDir = new File(artifactDir1, addedApp);

            addZombieFile(addedApp, artifactDir);

            String msg = miniSplash(String.format("Failed to deploy exploded artifact: '%s', see below", addedApp));
            logger.error(msg, t);

            deploymentListener.onDeploymentFailure(addedApp, t);

            if (t instanceof DeploymentException)
            {
                throw (DeploymentException) t;
            }
            else
            {
                msg = "Failed to deploy artifact: " + addedApp;
                throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
            }
        }

        deployArtifact(artifact);
    }

    protected void onArtifactInstalled(T a)
    {
        trackArtifact(a);
    }

    public void undeploy(String artifactName)
    {
        if (artifactZombieMap.containsKey(artifactName))
        {
            return;
        }
        T artifact = (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
        undeploy(artifact);
    }

    public void deploy(URL artifactAchivedUrl) throws IOException
    {
        T artifact;

        try
        {
            try
            {
                artifact = guardedInstallFrom(artifactAchivedUrl);
                trackArtifact(artifact);
            }
            catch (Throwable t)
            {
                File artifactArchive = new File(artifactAchivedUrl.toURI());
                String artifactName = StringUtils.removeEnd(artifactArchive.getName(), ZIP_FILE_SUFFIX);

                //// error text has been created by the deployer already
                final String msg = miniSplash(String.format("Failed to deploy artifact '%s', see below", artifactName));
                logger.error(msg, t);

                addZombieFile(artifactName, artifactArchive);

                deploymentListener.onDeploymentFailure(artifactName, t);

                throw t;
            }

            deployArtifact(artifact);
        }
        catch (Throwable t)
        {
            if (t instanceof DeploymentException)
            {
                // re-throw
                throw ((DeploymentException) t);
            }

            final String msg = "Failed to deploy from URL: " + artifactAchivedUrl;
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    private void guardedDeploy(T artifact)
    {
        try
        {
            if (!deploymentInProgressLcok.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deployer.deploy(artifact);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (deploymentInProgressLcok.isHeldByCurrentThread())
            {
                deploymentInProgressLcok.unlock();
            }
        }
    }

    public void deployArtifact(T artifact) throws DeploymentException
    {
        try
        {
            deploymentListener.onDeploymentStart(artifact.getArtifactName());
            guardedDeploy(artifact);
            deploymentListener.onDeploymentSuccess(artifact.getArtifactName());
            artifactZombieMap.remove(artifact.getArtifactName());
        }
        catch (Throwable t)
        {
            // error text has been created by the deployer already
            String msg = miniSplash(String.format("Failed to deploy artifact '%s', see below", artifact.getArtifactName()));
            logger.error(msg, t);

            addZombieApp(artifact);

            deploymentListener.onDeploymentFailure(artifact.getArtifactName(), t);
            if (t instanceof DeploymentException)
            {
                throw (DeploymentException) t;
            }
            else
            {
                msg = "Failed to deploy artifact: " + artifact.getArtifactName();
                throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
            }
        }
    }

    protected void addZombieApp(Artifact artifact)
    {
        File resourceFile = artifact.getConfigResourcesFile()[0];
        ZombieFile zombieFile = new ZombieFile();

        if (resourceFile.exists())
        {
            try
            {
                zombieFile.url = resourceFile.toURI().toURL();
                zombieFile.lastUpdated = resourceFile.lastModified();

                artifactZombieMap.put(artifact.getArtifactName(), zombieFile);
            }
            catch (MalformedURLException e)
            {
                // Ignore resource
            }
        }
    }

    protected void addZombieFile(String artifactName, File marker)
    {
        // no sync required as deploy operations are single-threaded
        if (marker == null)
        {
            return;
        }

        if (!marker.exists())
        {
            return;
        }

        try
        {
            long lastModified = marker.lastModified();

            ZombieFile zombieFile = new ZombieFile();
            zombieFile.url = marker.toURI().toURL();
            zombieFile.lastUpdated = lastModified;

            artifactZombieMap.put(artifactName, zombieFile);
        }
        catch (MalformedURLException e)
        {
            logger.debug(String.format("Failed to mark an exploded artifact [%s] as a zombie", marker.getName()), e);
        }
    }

    public T findArtifact(String artifactName)
    {
        return (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
    }

    private void trackArtifact(T artifact)
    {
        T previousArtifact = findArtifact(artifact.getArtifactName());
        artifacts.remove(previousArtifact);

        artifacts.add(artifact);
    }

    protected void undeploy(T artifact)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== Request to Undeploy Artifact: " + artifact.getArtifactName());
        }

        try
        {
            deploymentListener.onUndeploymentStart(artifact.getArtifactName());

            artifacts.remove(artifact);
            guardedUndeploy(artifact);

            deploymentListener.onUndeploymentSuccess(artifact.getArtifactName());
        }
        catch (RuntimeException e)
        {
            deploymentListener.onUndeploymentFailure(artifact.getArtifactName(), e);
            throw e;
        }
    }

    private T guardedInstallFrom(URL artifactUrl) throws IOException
    {
        try
        {
            if (!deploymentInProgressLcok.tryLock(0, TimeUnit.SECONDS))
            {
                throw new IOException(ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS);
            }
            return deployer.installFrom(artifactUrl);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(INSTALL_OPERATION_HAS_BEEN_INTERRUPTED);
        }
        finally
        {
            if (deploymentInProgressLcok.isHeldByCurrentThread())
            {
                deploymentInProgressLcok.unlock();
            }
        }
    }

    private void guardedUndeploy(T artifact)
    {
        try
        {
            if (!deploymentInProgressLcok.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deployer.undeploy(artifact);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (deploymentInProgressLcok.isHeldByCurrentThread())
            {
                deploymentInProgressLcok.unlock();
            }
        }
    }

    public Map<URL, Long> getArtifactsZombieMap()
    {
        Map<URL, Long> result = new HashMap<URL, Long>();

        for (String artifact : artifactZombieMap.keySet())
        {
            ZombieFile file = artifactZombieMap.get(artifact);
            result.put(file.url, file.lastUpdated);
        }
        return result;
    }

    public void setDeployer(MuleDeployer deployer)
    {
        this.deployer = deployer;
    }

    public void setArtifactFactory(ArtifactFactory<T> artifactFactory)
    {
        this.artifactFactory = artifactFactory;
    }

    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    public MuleDeployer getDeployer()
    {
        return deployer;
    }

    public void redeploy(T artifact)
    {
        artifact.redeploy();
        //anchor creation should not be required here once MULE-7126 gets fixed.
        File anchorFile = new File(artifactFactory.getArtifactDir(), String.format("%s%s", artifact.getArtifactName(), MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX));
        try
        {
            FileUtils.writeStringToFile(anchorFile, DefaultMuleDeployer.ANCHOR_FILE_BLURB);
        }
        catch (Throwable t)
        {
            artifact.dispose();
            if (t instanceof DeploymentException)
            {
                // re-throw as is
                throw ((DeploymentException) t);
            }
            final String msg = String.format("Failed to deploy artifact [%s]", artifact.getArtifactName());
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
        artifactZombieMap.remove(artifact.getArtifactName());
    }

    private static class ZombieFile
    {

        URL url;
        Long lastUpdated;

        public boolean isFor(URL url)
        {
            return this.url.equals(url);
        }

        public boolean updatedZombieApp()
        {
            long currentTimeStamp = FileUtils.getFileTimeStamp(url);
            return lastUpdated != currentTimeStamp;
        }
    }
}

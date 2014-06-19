/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.util.SplashScreen.miniSplash;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.application.NullDeploymentListener;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.artifact.ArtifactFactory;
import org.mule.module.launcher.util.ObservableList;
import org.mule.util.CollectionUtils;
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
public class DefaultArchiveDeployer<T extends Artifact> implements ArchiveDeployer<T>
{

    public static final String ARTIFACT_NAME_PROPERTY = "artifactName";
    public static final String ZIP_FILE_SUFFIX = ".zip";
    public static final String ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS = "Another deployment operation is in progress";
    public static final String INSTALL_OPERATION_HAS_BEEN_INTERRUPTED = "Install operation has been interrupted";
    private static final Log logger = LogFactory.getLog(DefaultArchiveDeployer.class);

    private final ArtifactDeployer<T> deployer;
    private final ArtifactArchiveInstaller artifactArchiveInstaller;
    private final ReentrantLock deploymentLock;
    private final Map<String, ZombieFile> artifactZombieMap = new HashMap<String, ZombieFile>();
    private final File artifactDir;
    private final ObservableList<T> artifacts;
    private ArtifactFactory<T> artifactFactory;
    private DeploymentListener deploymentListener = new NullDeploymentListener();


    public DefaultArchiveDeployer(final ArtifactDeployer deployer, final ArtifactFactory artifactFactory, final ObservableList<T> artifacts, final ReentrantLock lock)
    {
        this.deployer = deployer;
        this.artifactFactory = artifactFactory;
        this.artifacts = artifacts;
        this.deploymentLock = lock;
        this.artifactDir = artifactFactory.getArtifactDir();
        this.artifactArchiveInstaller = new ArtifactArchiveInstaller(artifactDir);
    }

    public T deployPackagedArtifact(String zip) throws DeploymentException
    {
        URL url;
        File artifactZip;
        try
        {
            final String artifactName = StringUtils.removeEnd(zip, ZIP_FILE_SUFFIX);
            artifactZip = new File(artifactDir, zip);
            url = artifactZip.toURI().toURL();
            return deployPackagedArtifact(url, artifactName);
        }
        catch (DeploymentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DeploymentException(CoreMessages.createStaticMessage("Failed to deploy from zip: " + zip), e);
        }
    }

    public T deployExplodedArtifact(String artifactDir) throws DeploymentException
    {
        String artifactName = artifactDir;
        @SuppressWarnings("rawtypes")
        Collection<String> deployedAppNames = CollectionUtils.collect(artifacts, new BeanToPropertyValueTransformer(ARTIFACT_NAME_PROPERTY));

        if (deployedAppNames.contains(artifactName) && (!artifactZombieMap.containsKey(artifactName)))
        {
            return null;
        }

        ZombieFile zombieFile = artifactZombieMap.get(artifactName);

        if ((zombieFile != null) && (!zombieFile.updatedZombieApp()))
        {
            return null;
        }

        return deployExplodedApp(artifactName);
    }

    public void undeployArtifact(String artifactId)
    {
        ZombieFile zombieFile = artifactZombieMap.get(artifactId);
        if ((zombieFile != null))
        {
            return;
        }

        T artifact = (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactId));
        undeploy(artifact);
    }

    @Override
    public File getDeploymentDirectory()
    {
        return artifactFactory.getArtifactDir();
    }

    public T deployPackagedArtifact(URL artifactAchivedUrl) throws DeploymentException
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

                // error text has been created by the deployer already
                logDeploymentFailure(t, artifactName);

                addZombieFile(artifactName, artifactArchive);

                deploymentListener.onDeploymentFailure(artifactName, t);

                throw t;
            }

            deployArtifact(artifact);
            return artifact;
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

    private void logDeploymentFailure(Throwable t, String artifactName)
    {
        final String msg = miniSplash(String.format("Failed to deploy artifact '%s', see below", artifactName));
        logger.error(msg, t);
    }

    public Map<URL, Long> getArtifactsZombieMap()
    {
        Map<URL, Long> result = new HashMap<URL, Long>();

        for (String artifact : artifactZombieMap.keySet())
        {
            ZombieFile file = artifactZombieMap.get(artifact);
            result.put(file.url, file.originalTimestamp);
        }
        return result;
    }

    public void setArtifactFactory(final ArtifactFactory<T> artifactFactory)
    {
        this.artifactFactory = artifactFactory;
    }

    @Override
    public void undeployArtifactWithoutUninstall(T artifact)
    {
        logRequestToUndeployArtifact(artifact);
        try
        {
            if (!deploymentLock.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deploymentListener.onUndeploymentStart(artifact.getArtifactName());
            deployer.undeploy(artifact);
            deploymentListener.onUndeploymentSuccess(artifact.getArtifactName());
        }
        catch (DeploymentException e)
        {
            deploymentListener.onUndeploymentFailure(artifact.getArtifactName(), e);
            throw e;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (deploymentLock.isHeldByCurrentThread())
            {
                deploymentLock.unlock();
            }
        }
    }

    ArtifactDeployer getDeployer()
    {
        return deployer;
    }

    public void setDeploymentListener(CompositeDeploymentListener deploymentListener)
    {
        this.deploymentListener = deploymentListener;
    }

    private T deployPackagedArtifact(final URL artifactUrl, String artifactName) throws IOException
    {
        ZombieFile zombieFile = artifactZombieMap.get(artifactName);
        if (zombieFile != null)
        {
            if (zombieFile.isFor(artifactUrl) && !zombieFile.updatedZombieApp())
            {
                // Skips the file because it was already deployed with failure
                return null;
            }
        }

        // check if this artifact is running first, undeployArtifact it then
        T artifact = (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
        if (artifact != null)
        {
            undeployArtifact(artifactName);
        }

        return deployPackagedArtifact(artifactUrl);
    }

    private T deployExplodedApp(String addedApp) throws DeploymentException
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
            trackArtifact(artifact);
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
        return artifact;
    }


    private void guardedDeploy(T artifact)
    {
        try
        {
            if (!deploymentLock.tryLock(0, TimeUnit.SECONDS))
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
            if (deploymentLock.isHeldByCurrentThread())
            {
                deploymentLock.unlock();
            }
        }
    }

    public void deployArtifact(T artifact) throws DeploymentException
    {
        try
        {
            deploymentListener.onDeploymentStart(artifact.getArtifactName());
            guardedDeploy(artifact);
            artifactArchiveInstaller.createAnchorFile(artifact.getArtifactName());
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

    private void addZombieApp(Artifact artifact)
    {
        File resourceFile = artifact.getResourceFiles()[0];

        if (resourceFile.exists())
        {
            try
            {
                artifactZombieMap.put(artifact.getArtifactName(), new ZombieFile(resourceFile));
            }
            catch (Exception e)
            {
                // ignore resource
            }
        }
    }

    private void addZombieFile(String artifactName, File marker)
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
            artifactZombieMap.put(artifactName, new ZombieFile(marker));
        }
        catch (Exception e)
        {
            logger.debug(String.format("Failed to mark an exploded artifact [%s] as a zombie", marker.getName()), e);
        }
    }

    private T findArtifact(String artifactName)
    {
        return (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
    }

    private void trackArtifact(T artifact)
    {
        T previousArtifact = findArtifact(artifact.getArtifactName());
        artifacts.remove(previousArtifact);

        artifacts.add(artifact);
    }

    private void undeploy(T artifact)
    {
        logRequestToUndeployArtifact(artifact);
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

    private void logRequestToUndeployArtifact(T artifact)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== Request to Undeploy Artifact: " + artifact.getArtifactName());
        }
    }

    private T guardedInstallFrom(URL artifactUrl) throws IOException
    {
        try
        {
            if (!deploymentLock.tryLock(0, TimeUnit.SECONDS))
            {
                throw new IOException(ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS);
            }
            return installFrom(artifactUrl);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(INSTALL_OPERATION_HAS_BEEN_INTERRUPTED);
        }
        finally
        {
            if (deploymentLock.isHeldByCurrentThread())
            {
                deploymentLock.unlock();
            }
        }
    }

    private T installFrom(URL url) throws IOException
    {
        String artifactName = artifactArchiveInstaller.installArtifact(url);
        return artifactFactory.createArtifact(artifactName);
    }

    private void guardedUndeploy(T artifact)
    {
        try
        {
            if (!deploymentLock.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deployer.undeploy(artifact);
            artifactArchiveInstaller.desinstallArtifact(artifact.getArtifactName());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (deploymentLock.isHeldByCurrentThread())
            {
                deploymentLock.unlock();
            }
        }
    }

    public void redeploy(T artifact) throws DeploymentException
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Redeploying artifact '%s'", artifact.getArtifactName())));
        }

        deploymentListener.onUndeploymentStart(artifact.getArtifactName());
        try
        {
            deployer.undeploy(artifact);
            deploymentListener.onUndeploymentSuccess(artifact.getArtifactName());
        }
        catch (Throwable e)
        {
            //TODO make the exception beter
            deploymentListener.onUndeploymentFailure(artifact.getArtifactName(), e);
        }

        deploymentListener.onDeploymentStart(artifact.getArtifactName());
        try
        {
            deployer.deploy(artifact);
            artifactArchiveInstaller.createAnchorFile(artifact.getArtifactName());
            deploymentListener.onDeploymentSuccess(artifact.getArtifactName());
        }
        catch (Throwable t)
        {
            try
            {
                logDeploymentFailure(t, artifact.getArtifactName());
                if (t instanceof DeploymentException)
                {
                    throw (DeploymentException) t;
                }
                String msg = "Failed to deploy artifact: " + artifact.getArtifactName();
                throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
            }
            finally
            {
                deploymentListener.onDeploymentFailure(artifact.getArtifactName(), t);
            }
        }

        artifactZombieMap.remove(artifact.getArtifactName());
    }

    private static class ZombieFile
    {

        URL url;
        Long originalTimestamp;
        File file;

        private ZombieFile(File file)
        {
            this.file = file;
            originalTimestamp = file.lastModified();
            try
            {
                url = file.toURI().toURL();
            }
            catch (MalformedURLException e)
            {
                throw new IllegalArgumentException(e);
            }
        }

        public boolean isFor(URL url)
        {
            return this.url.equals(url);
        }

        public boolean updatedZombieApp()
        {
            return originalTimestamp != file.lastModified();
        }
    }
}

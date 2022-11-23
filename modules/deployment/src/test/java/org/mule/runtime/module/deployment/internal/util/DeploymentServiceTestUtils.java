package org.mule.runtime.module.deployment.internal.util;

import org.mule.runtime.module.deployment.api.DeploymentService;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

/**
 * Utility class used to avoid skipping actions in the MuleDeploymentService#executeSynchronized method. We avoid it by
 * acquiring the lock before calling the actual method.
 */
public final class DeploymentServiceTestUtils {

    private DeploymentServiceTestUtils() {
    }

    /**
     * Undeploys an application from the mule container
     *
     * @param appName name of the application to undeploy
     */
    public static void undeploy(DeploymentService delegate, String appName) {
        delegate.getLock().lock();
        try {
            delegate.undeploy(appName);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Deploys an application bundled as a zip from the given URL to the mule container
     *
     * @param appArchiveUri location of the zip application file
     * @throws IOException
     */
    public static void deploy(DeploymentService delegate, URI appArchiveUri) throws IOException {
        delegate.getLock().lock();
        try {
            delegate.deploy(appArchiveUri);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Deploys an application bundled as a zip from the given URL to the mule container and applies the provided properties.
     *
     * @param appArchiveUri location of the zip application file
     * @param appProperties map of properties to include
     * @throws IOException
     */
    public static void deploy(DeploymentService delegate, URI appArchiveUri, Properties appProperties) throws IOException {
        delegate.getLock().lock();
        try {
            delegate.deploy(appArchiveUri, appProperties);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Undeploys and redeploys an application
     *
     * @param artifactName then name of the application to redeploy
     */
    public static void redeploy(DeploymentService delegate, String artifactName) {
        delegate.getLock().lock();
        try {
            delegate.redeploy(artifactName);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Undeploys and redeploys an application including the provided appProperties.
     *
     * @param artifactName  then name of the application to redeploy
     * @param appProperties map of properties to include
     */
    public static void redeploy(DeploymentService delegate, String artifactName, Properties appProperties) {
        delegate.getLock().lock();
        try {
            delegate.redeploy(artifactName, appProperties);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Undeploys and redeploys an application using a new artifact URI and including the provided appProperties.
     *
     * @param archiveUri    location of the application file
     * @param appProperties map of properties to include
     */
    public static void redeploy(DeploymentService delegate, URI archiveUri, Properties appProperties) throws IOException {
        delegate.getLock().lock();
        try {
            delegate.redeploy(archiveUri, appProperties);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Undeploys and redeploys an application using a new artifact URI.
     *
     * @param archiveUri location of the application file
     */
    public static void redeploy(DeploymentService delegate, URI archiveUri) throws IOException {
        delegate.getLock().lock();
        try {
            delegate.redeploy(archiveUri);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Undeploys a domain from the mule container
     *
     * @param domainName name of the domain to undeploy
     */
    public static void undeployDomain(DeploymentService delegate, String domainName) {
        delegate.getLock().lock();
        try {
            delegate.undeployDomain(domainName);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Deploys a domain artifact from the given URL to the mule container
     *
     * @param domainArchiveUri location of the domain file
     * @throws IOException
     */
    public static void deployDomain(DeploymentService delegate, URI domainArchiveUri) throws IOException {
        delegate.getLock().lock();
        try {
            delegate.deployDomain(domainArchiveUri);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Deploys a domain bundled as a zip from the given URL to the mule container
     *
     * @param domainArchiveUri     location of the zip domain file.
     * @param deploymentProperties the properties to override during the deployment process.
     * @throws IOException
     */
    public static void deployDomain(DeploymentService delegate, URI domainArchiveUri, Properties deploymentProperties) throws IOException {
        delegate.getLock().lock();
        try {
            delegate.deployDomain(domainArchiveUri, deploymentProperties);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Undeploys and redeploys a domain
     *
     * @param domainName           then name of the domain to redeploy.
     * @param deploymentProperties the properties to override during the deployment process.
     */
    public static void redeployDomain(DeploymentService delegate, String domainName, Properties deploymentProperties) {
        delegate.getLock().lock();
        try {
            delegate.redeployDomain(domainName, deploymentProperties);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Undeploys and redeploys a domain
     *
     * @param domainName then name of the domain to redeploy
     */
    public static void redeployDomain(DeploymentService delegate, String domainName) {
        delegate.getLock().lock();
        try {
            delegate.redeployDomain(domainName);
        } finally {
            delegate.getLock().unlock();
        }
    }

    /**
     * Deploys a domain bundle from the given URL to the mule container
     *
     * @param domainArchiveUri location of the ZIP domain file
     * @throws IOException if there is any problem reading the file
     */
    public static void deployDomainBundle(DeploymentService delegate, URI domainArchiveUri) throws IOException {
        delegate.getLock().lock();
        try {
            delegate.deployDomainBundle(domainArchiveUri);
        } finally {
            delegate.getLock().unlock();
        }
    }
}

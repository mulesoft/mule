package org.mule.module.launcher;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringMessageUtils;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Meta data is commandline options.
 */
public class MuleAppDeployer implements Deployer<Map<String, Object>>
{

    public static final String DEFAULT_CONFIGURATION = "mule-config.xml";
    /**
     * Default dev-mode builder with hot-deployment.
     */
    public static final String CLASSNAME_DEV_MODE_CONFIG_BUILDER = "org.mule.config.spring.hotdeploy.ReloadableBuilder";

    /**
     * Required to support the '-config spring' shortcut. Don't use a class object so
     * the core doesn't depend on mule-module-spring.
     */
    protected static final String CLASSNAME_SPRING_CONFIG_BUILDER = "org.mule.config.spring.SpringXmlConfigurationBuilder";

    protected transient final Log logger = LogFactory.getLog(getClass());

    private String appName;
    private Map<String, Object> metaData;
    private String configBuilderClassName;
    protected URL configUrl;
    private MuleContext muleContext;
    private ClassLoader deploymentClassLoader;

    public MuleAppDeployer(String appName)
    {
        this.appName = appName;
    }

    public void install()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Installing application: " + appName);
        }

        final String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
        // try to load the config as a file as well
        final String configPath = String.format("%s/apps/%s/%s", muleHome, getAppName(), MuleAppDeployer.DEFAULT_CONFIGURATION);
        configUrl = IOUtils.getResourceAsUrl(configPath, getClass(), true, false);
        if (configUrl == null)
        {
            System.out.println(CoreMessages.configNotFoundUsage());
            // TODO replace with a deployment exception
            System.exit(-1);
        }

        // TODO replace with a hotdeploy switch
        final String productionMode = (String) metaData.get("production");
        //if (productionMode == null)
        //{
        try
        {
            this.configBuilderClassName = CLASSNAME_DEV_MODE_CONFIG_BUILDER;
        }
        catch (Exception e)
        {
            logger.fatal(e);
            final Message message = CoreMessages.failedToLoad("Builder: " + CLASSNAME_DEV_MODE_CONFIG_BUILDER);
            System.err.println(StringMessageUtils.getBoilerPlate("FATAL: " + message.toString()));
            // TODO replace with a deployment exception
            System.exit(1);
        }
        //}

        // TODO discover it from app descriptor?
        // Configuration builder
        //String cfgBuilderClassName = (String) commandlineOptions.get("builder");

        // Configuration builder
        try
        {
            // Provide a shortcut for Spring: "-builder spring"
            if (configBuilderClassName.equalsIgnoreCase("spring"))
            {
                this.configBuilderClassName = CLASSNAME_SPRING_CONFIG_BUILDER;
            }
        }
        catch (Exception e)
        {
            logger.fatal(e);
            final Message message = CoreMessages.failedToLoad("Builder: " + this.configBuilderClassName);
            System.err.println(StringMessageUtils.getBoilerPlate("FATAL: " + message.toString()));
            // TODO replace with a deployment exception
            System.exit(1);
        }

        ClassLoader parent = new DefaultMuleSharedDomainClassLoader(getClass().getClassLoader());
        this.deploymentClassLoader = new MuleApplicationClassLoader(appName, new File(configUrl.getFile()), parent);
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public void start()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Starting application: " + appName);
        }

        try
        {
            this.muleContext.start();
        }
        catch (MuleException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStartException(MessageFactory.createStaticMessage(appName), e);
        }
    }

    public void init()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Initializing application: " + appName);
        }

        try
        {
            //Thread.currentThread().setContextClassLoader(null);|
            //Thread.currentThread().setContextClassLoader(cl);

            // create a new ConfigurationBuilder that is disposed afterwards
            ConfigurationBuilder cfgBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(configBuilderClassName,
                                                                                                 new Object[] {configUrl.toExternalForm()}, getDeploymentClassLoader());
            if (!cfgBuilder.isConfigured())
            {
                //List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(2);
                //builders.add(cfgBuilder);

                // If the annotations module is on the classpath, add the annotations config builder to the list
                // This will enable annotations config for this instance
                //if (ClassUtils.isClassOnPath(CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
                //{
                //    Object configBuilder = ClassUtils.instanciateClass(
                //            CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, ClassUtils.NO_ARGS, getClass());
                //    builders.add((ConfigurationBuilder) configBuilder);
                //}

                DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
                // TODO properties for the app should come from the app descriptor
                this.muleContext = muleContextFactory.createMuleContext(cfgBuilder);
            }
        }
        catch (Exception e)
        {
            throw new DeploymentInitException(CoreMessages.failedToLoad(configBuilderClassName), e);
        }
    }

    public void setMetaData(Map<String, Object> metaData)
    {
        this.metaData = metaData;
    }

    public Map<String, Object> getMetaData()
    {
        return this.metaData;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public ClassLoader getDeploymentClassLoader()
    {
        return this.deploymentClassLoader;
    }

    public void dispose()
    {

        if (muleContext.isStarted() && !muleContext.isDisposed())
        {
            stop();
        }
        if (logger.isInfoEnabled())
        {
            logger.info("Disposing application: " + appName);
        }

        muleContext.dispose();
    }

    public void restart()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Restarting application: " + appName);
        }
        stop();
        start();
    }

    public void stop()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Stopping application: " + appName);
        }
        try
        {
            this.muleContext.stop();
        }
        catch (MuleException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStopException(MessageFactory.createStaticMessage(appName), e);
        }
    }

    protected class ConfigFileWatcher extends FileWatcher
    {

        private final MuleContext muleContext;

        public ConfigFileWatcher(MuleContext muleContext)
        {
            super(new File(configUrl.getFile()));
            this.muleContext = muleContext;
        }

        protected synchronized void onChange(File file)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("================== Reloading " + file);
            }


            try
            {
                restart();
                /*muleContext.dispose();
                Thread.currentThread().setContextClassLoader(null);
                // TODO this is really a job of a deployer and deployment descriptor info
                // TODO I don't think shared domains can be safely redeployed, this will probably be removed
                ClassLoader parent = MuleBootstrapUtils.isStandalone()
                                     ? new DefaultMuleSharedDomainClassLoader(CLASSLOADER_ROOT)
                                     : CLASSLOADER_ROOT;
                ClassLoader cl = new MuleApplicationClassLoader(monitoredResource, parent);
                Thread.currentThread().setContextClassLoader(cl);

                DefaultMuleContextFactory f = new DefaultMuleContextFactory();
                MuleContext newContext = f.createMuleContext(ReloadableBuilder.this);
                newContext.start();*/
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
            //finally
            //{
            //    Thread.currentThread().setContextClassLoader(rootClassloader);
            //}

        }
    }

}

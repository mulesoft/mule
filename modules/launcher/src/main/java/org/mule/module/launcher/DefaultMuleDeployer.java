package org.mule.module.launcher;

import org.mule.config.StartupContext;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class DefaultMuleDeployer
{
    private List<Application> applications = new ArrayList<Application>();

    public void deploy()
    {
        // install phase
        final Map<String, Object> options = StartupContext.get().getStartupOptions();
        String appString = (String) options.get("app");

        String[] apps;
        if (appString == null)
        {
            apps = MuleContainerBootstrapUtils.getMuleAppsFile().list();
        }
        else
        {
            apps = appString.split(":");
        }

        for (String app : apps)
        {
            final ApplicationWrapper<Map<String, Object>> a = new ApplicationWrapper<Map<String, Object>>(new DefaultMuleApplication(app));
            a.setMetaData(options);
            applications.add(a);
        }

        for (Application application : applications)
        {
            try
            {
                application.install();
                application.init();
                application.start();
            }
            catch (Throwable t)
            {
                // TODO logging
                t.printStackTrace();
            }
        }

    }

    public void dispose()
    {
        // tear down apps in reverse order
        Collections.reverse(applications);
        for (Application application : applications)
        {
            try
            {
                application.stop();
                application.dispose();
            }
            catch (Throwable t)
            {
                // TODO logging
                t.printStackTrace();
            }
        }
    }
}

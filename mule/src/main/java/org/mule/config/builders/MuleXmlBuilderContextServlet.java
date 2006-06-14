/*
 * Created on Jun 14, 2005
 *
 */
package org.mule.config.builders;

import org.mule.MuleManager;
import org.mule.config.ConfigurationException;
import org.mule.umo.manager.UMOManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author EAF Team
 *
 */
public class MuleXmlBuilderContextServlet extends HttpServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2446689032349402434L;

    public static final String CONFIG_INIT_PARAMETER = "org.mule.config";

    public void init() throws ServletException
    {
        try
        {
            String config = getServletContext().getInitParameter(CONFIG_INIT_PARAMETER);
            if(config == null)
            {
                config = getDefaultConfigResource();
            }

            createManager(config, getServletContext());
        }
        catch (ConfigurationException e)
        {
            getServletContext().log(e.getMessage(), e);
        }
    }

    /**
     * Used to actually construct the UMOManager instance
     * @param configResource  the location of the config resource, this can be on the
     * local file system or on the classpath.
     * @return  A configured UMOManager instance
     */
    protected UMOManager createManager(String configResource, ServletContext context) throws ConfigurationException {
        WebappMuleXmlConfigurationBuilder builder = new WebappMuleXmlConfigurationBuilder(context);
        return builder.configure(configResource);
    }

    /**
     * If no config location resource is configured on the servlet context, the
     * value returned from this method will be used to initialise the MuleManager.
     * @return the default config resource location
     */
    protected String getDefaultConfigResource()
    {
        return "/WEB-INF/mule-config.xml";
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        getServletContext().log("(" + request.getRequestURI() + ")" + "MuleXmlBuilderContextServlet.service(HttpServletRequest request, HttpServletResponse response) call ignored.");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    public void destroy()
    {
        MuleManager.getInstance().dispose();
    }
}

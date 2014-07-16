/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.agent;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * <code>AbstractNotificationLoggerAgent</code> Receives Mule server notifications
 * and logs them and can optionally route them to an endpoint
 */
public class Log4jNotificationLoggerAgent extends AbstractNotificationLoggerAgent
{
    protected static final int DEFAULT_DESCRIPTION_BUFFER_SIZE = 64;

    protected Logger eventLogger;
    private String logName = Log4jNotificationLoggerAgent.class.getName();
    private String logFile = null;
    private String logConfigFile = null;
    private String chainsawHost = "localhost";
    private int chainsawPort = -1;
    private final Map<String, String> levelMappings = new HashMap<String, String>();

    public Log4jNotificationLoggerAgent()
    {
        super("log4j-notifications");
    }

    /**
     * Should be a 1 line description of the agent
     */
    @Override
    public String getDescription()
    {
        StringBuilder buf = new StringBuilder(DEFAULT_DESCRIPTION_BUFFER_SIZE);

        if (StringUtils.isNotBlank(logName))
        {
            buf.append("Logging notifications to logger: ").append(logName);
        }

        if (StringUtils.isNotBlank(logFile))
        {
            buf.append("Logging notifications to: ").append(logFile);
        }

        if (chainsawPort > -1)
        {
            buf.append(" Chainsaw: ").append(chainsawHost).append(":").append(chainsawPort);
        }

        if (buf.length() == 0)
        {
            buf.append("No logging or event forwarding is configured");
        }

        return getName() + ": " + buf.toString();
    }

    public String getLogName()
    {
        return logName;
    }

    public void setLogName(String logName)
    {
        this.logName = logName;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (logConfigFile != null)
        {
            if (logConfigFile.endsWith(".xml"))
            {
                DOMConfigurator.configure(logConfigFile);
            }
            else
            {
                PropertyConfigurator.configure(logConfigFile);
            }
        }
        else
        {
            eventLogger = Logger.getLogger(logName);

            /*
             * TODO PAX Logging's Log4J version does not have the method
             * Logger.addAppender() and does not export the package
             * org.apache.log4j.net
             */
            // try
            // {
            // if (logFile != null)
            // {
            // File f = FileUtils.newFile(logFile);
            // if (!f.exists())
            // {
            // FileUtils.createFile(logFile);
            // }
            // Appender file = new RollingFileAppender(new PatternLayout("%5p %m%n"),
            // logFile, true);
            // eventLogger.addAppender(file);
            // }
            // if (chainsawPort > -1)
            // {
            // Appender chainsaw = new SocketAppender(chainsawHost, chainsawPort);
            // eventLogger.addAppender(chainsaw);
            // }
            // }
            // catch (IOException e)
            // {
            // throw new InitialisationException(
            // CoreMessages.failedToLoad("Log4j configuration"), e, this);
            // }
        }

    }

    @Override
    protected void logEvent(ServerNotification e)
    {
        if (eventLogger != null)
        {
            String actionKey = e.EVENT_NAME + "." + e.getActionName();
            String level = MapUtils.getString(levelMappings, actionKey, e.getType());

            eventLogger.log(Level.toLevel(level, Level.INFO), e);
        }
    }

    public String getLogFile()
    {
        return logFile;
    }

    public void setLogFile(String logFile)
    {
        this.logFile = logFile;
    }

    public String getLogConfigFile()
    {
        return logConfigFile;
    }

    public void setLogConfigFile(String logConfigFile)
    {
        this.logConfigFile = logConfigFile;
    }

    public String getChainsawHost()
    {
        return chainsawHost;
    }

    public void setChainsawHost(String chainsawHost)
    {
        this.chainsawHost = chainsawHost;
    }

    public int getChainsawPort()
    {
        return chainsawPort;
    }

    public void setChainsawPort(int chainsawPort)
    {
        this.chainsawPort = chainsawPort;
    }

    public Map<String, String> getLevelMappings()
    {
        return levelMappings;
    }

    public void setLevelMappings(Map<String, String> levelMappings)
    {
        this.levelMappings.putAll(levelMappings);
    }
}

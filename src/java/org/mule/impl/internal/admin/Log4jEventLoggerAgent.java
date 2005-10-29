/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.internal.admin;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.net.SocketAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerEvent;
import org.mule.util.PropertiesHelper;
import org.mule.util.Utility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>AbstractEventLoggerAgent</code> Receives Mule server events and logs
 * them and can optionally route them to an endpoint
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Log4jEventLoggerAgent extends AbstractEventLoggerAgent
{

    protected Logger eventLogger;
    private String logName = Log4jEventLoggerAgent.class.getName();
    private String logFile = null;
    private String logConfigFile = null;
    private String chainsawHost = "localhost";
    private int chainsawPort = 4445;
    private boolean enableChainsaw = false;
    private Map levelMappings = new HashMap();

    /**
     * Should be a 1 line description of the agent
     * 
     * @return
     */
    public String getDescription()
    {
        StringBuffer buf = new StringBuffer();
        if (logFile != null && !"".equals(logFile)) {
            buf.append("Logging events to: ").append(logFile);
        }
        if (enableChainsaw) {
            buf.append(" Chainsaw: ").append(chainsawHost).append(":").append(chainsawPort);
        }
        if (buf.length() == 0) {
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

    public void doInitialise() throws InitialisationException
    {
        if (logConfigFile != null) {
            if (logConfigFile.endsWith(".xml")) {
                DOMConfigurator.configure(logConfigFile);
            } else {
                PropertyConfigurator.configure(logConfigFile);
            }
        } else {
            try {
                eventLogger = Logger.getLogger(logName);
                if (logFile != null) {
                    File f = new File(logFile);
                    if (!f.exists()) {
                        Utility.createFile(logFile);
                    }
                    Appender file = new RollingFileAppender(new PatternLayout("%5p %m%n"), logFile, true);
                    eventLogger.addAppender(file);
                }
                if (enableChainsaw) {
                    Appender chainsaw = new SocketAppender(chainsawHost, chainsawPort);
                    eventLogger.addAppender(chainsaw);
                }
            } catch (IOException e) {
                throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Log4j configuration"), e, this);
            }
        }
    }

    protected void logEvent(UMOServerEvent e)
    {
        if (eventLogger != null) {
            String actionKey = e.EVENT_NAME + "." + e.getActionName();
            String level = (String) PropertiesHelper.getProperty(levelMappings, actionKey, e.getType());

            eventLogger.log(getPriority(level), e);
        }
    }

    private Priority getPriority(String level)
    {
        if (level.equalsIgnoreCase("debug")) {
            return Priority.DEBUG;
        } else if (level.equalsIgnoreCase("warn")) {
            return Priority.WARN;
        } else if (level.equalsIgnoreCase("error")) {
            return Priority.ERROR;
        } else if (level.equalsIgnoreCase("fatal")) {
            return Priority.FATAL;
        } else {
            return Priority.INFO;
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

    public boolean isEnableChainsaw()
    {
        return enableChainsaw;
    }

    public void setEnableChainsaw(boolean enableChainsaw)
    {
        this.enableChainsaw = enableChainsaw;
    }

    public Map getLevelMappings()
    {
        return levelMappings;
    }

    public void setLevelMappings(Map levelMappings)
    {
        this.levelMappings.putAll(levelMappings);
    }
}

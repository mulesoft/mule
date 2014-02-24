/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;

public class MuleProcessController
{

    private static final String ANCHOR_DELETE_ERROR = "Could not delete anchor file [%s] when stopping Mule ESB.";
    private static final String ADD_LIBRARY_ERROR = "Error copying jar file [%s] to lib directory [%s].";
    private static final String CREATE_DOMAIN_ERROR = "Error creating domain [%s] with config [%s].";
    private static final String MULE_HOME_VARIABLE = "MULE_HOME";
    private static final String ANCHOR_SUFFIX = "-anchor.txt";
    private static final String STATUS = "Mule Enterprise Edition is running \\(([0-9]+)\\)\\.";
    private static final Pattern STATUS_PATTERN = Pattern.compile(STATUS);
    private static final IOFileFilter ANCHOR_FILTER = FileFilterUtils.suffixFileFilter(ANCHOR_SUFFIX);
    private static final int IS_RUNNING_STATUS_CODE = 0;
    public static final int TIMEOUT = 30000;
    private String muleHome;
    private String muleBin;
    private File domainsDir;
    private File appsDir;
    private File libsDir;

    public MuleProcessController(String muleHome)
    {
        this.muleHome = muleHome;
        this.muleBin = muleHome + "/bin/mule";
        this.domainsDir = new File(muleHome + "/domains");
        this.appsDir = new File(muleHome + "/apps/");
        this.libsDir = new File(muleHome + "/lib/user");
    }

    public boolean isRunning()
    {
        return IS_RUNNING_STATUS_CODE == status();
    }

    public void start(String... args)
    {
        int error = runSync("start", args);
        if (error != 0)
        {
            throw new MuleControllerException("The mule instance couldn't be started");
        }
    }

    public void stop(String... args)
    {
        int error = runSync("stop", args);
        verify(error == 0, "The mule instance couldn't be stopped");
        deleteAnchors();
    }

    private void deleteAnchors()
    {
        @SuppressWarnings("unchecked")
        Collection<File> anchors = FileUtils.listFiles(appsDir, ANCHOR_FILTER, null);
        for (File anchor : anchors)
        {
            try
            {
                FileUtils.forceDelete(anchor);
            }
            catch (IOException e)
            {
                throw new MuleControllerException(String.format(ANCHOR_DELETE_ERROR, anchor), e);
            }
        }
    }

    public int status(String... args)
    {
        return runSync("status", args);
    }

    public int getProcessId()
    {
        Map<Object, Object> newEnv = this.copyEnvironmentVariables();
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog((long) TIMEOUT);
        executor.setWatchdog(watchdog);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);
        if (this.doExecution(executor, new CommandLine(this.muleBin).addArgument("status"), newEnv) == 0)
        {
            Matcher matcher = STATUS_PATTERN.matcher(outputStream.toString());
            if (matcher.find())
            {
                return Integer.parseInt(matcher.group(1));
            }
            else
            {
                throw new MuleControllerException("bin/mule status didn't return the expected pattern: "
                                                  + STATUS);
            }
        }
        else
        {
            throw new MuleControllerException("Mule ESB is not running");
        }
    }

    public void restart(String... args)
    {
        int error = runSync("restart", args);
        if (error != 0)
        {
            throw new MuleControllerException("The mule instance couldn't be restarted");
        }
    }

    protected int runSync(String command, String... args)
    {
        Map<Object, Object> newEnv = copyEnvironmentVariables();
        return executeSyncCommand(command, args, newEnv, TIMEOUT);
    }

    private int executeSyncCommand(String command, String[] args, Map<Object, Object> newEnv, long timeout)
        throws MuleControllerException
    {
        CommandLine commandLine = new CommandLine(muleBin);
        commandLine.addArgument(command);
        commandLine.addArguments(args);
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);
        executor.setStreamHandler(new PumpStreamHandler());
        return doExecution(executor, commandLine, newEnv);
    }

    private int doExecution(DefaultExecutor executor, CommandLine commandLine, Map<Object, Object> env)
    {
        try
        {
            return executor.execute(commandLine, env);
        }
        catch (ExecuteException e)
        {
            return e.getExitValue();
        }
        catch (Exception e)
        {
            throw new MuleControllerException("Error executing [" + commandLine.getExecutable() + " "
                                              + commandLine.getArguments() + "]", e);
        }
    }

    public void deploy(String path)
    {
        File app = new File(path);
        verify(app.exists(), "File does not exists: %s", app);
        verify(app.canRead(), "Cannot read file: %s", app);
        try
        {
            if (app.isFile())
            {
                FileUtils.copyFileToDirectory(app, appsDir);
            }
            else
            {
                FileUtils.copyDirectoryToDirectory(app, appsDir);
            }
        }
        catch (IOException e)
        {
            throw new MuleControllerException("Could not deploy app [" + path + "] to [" + appsDir + "]", e);
        }
    }

    public boolean isDeployed(String appName)
    {
        return new File(appsDir, appName + ANCHOR_SUFFIX).exists();
    }

    private Map<Object, Object> copyEnvironmentVariables()
    {
        Map<String, String> env = System.getenv();
        Map<Object, Object> newEnv = new HashMap<Object, Object>();
        for (Map.Entry<String, String> it : env.entrySet())
        {
            newEnv.put(it.getKey(), it.getValue());
        }
        newEnv.put(MULE_HOME_VARIABLE, muleHome);
        return newEnv;
    }

    public void undeployAll()
    {
        for (File file : appsDir.listFiles())
        {
            try
            {
                FileUtils.forceDelete(file);
            }
            catch (IOException e)
            {
                throw new MuleControllerException("Could not delete directory [" + file.getAbsolutePath()
                                                  + "]", e);
            }
        }
    }

    public void installLicense(String path)
    {
        this.runSync(null, "--installLicense", path);
    }

    public void uninstallLicense()
    {
        this.runSync(null, "-unInstallLicense");
    }

    public void addLibrary(File jar)
    {
        verify(jar.exists(), "Jar file does not exist: %s", jar);
        verify("jar".equals(FilenameUtils.getExtension(jar.getAbsolutePath())), "Library [%s] don't have .jar extension.", jar);
        verify(jar.canRead(), "Cannot read jar file: %s", jar);
        verify(libsDir.canWrite(), "Cannot write on lib dir: %", libsDir);
        try
        {
            FileUtils.copyFileToDirectory(jar, libsDir);
        }
        catch (IOException e)
        {
            throw new MuleControllerException(String.format(ADD_LIBRARY_ERROR, jar, libsDir), e);
        }
    }

    private void verify(boolean condition, String message, Object... args)
    {
        if (!condition)
        {
            throw new MuleControllerException(String.format(message, args));
        }
    }

    public void createDomain(String domainName, File domainConfig)
    {
        verify(domainConfig.exists(), "Domain configuration file does not exist: %s", domainConfig);
        verify(StringUtils.isNotEmpty(domainName), "Domain name is empty");
        verify(domainConfig.exists(), "Domain configuration file does not exist: %s", domainConfig);
        File domain = new File(this.domainsDir, domainName);
        verify(!domain.exists(), "Domain %s already exists", domainName);
        verify(domain.mkdir(), "Couldn't create domain directory %s", domain);
        try
        {
            FileUtils.copyFileToDirectory(domainConfig, domain);
        }
        catch (IOException e)
        {
            throw new MuleControllerException(String.format(CREATE_DOMAIN_ERROR, domainName, domainConfig), e);
        }

    }

}

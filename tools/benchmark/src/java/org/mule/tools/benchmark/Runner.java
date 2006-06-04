/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.tools.benchmark;

import org.mule.MuleManager;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMOException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.PropertiesUtils;
import org.mule.util.timer.EventTimerTask;
import org.mule.util.timer.TimeEvent;
import org.mule.util.timer.TimeEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

/**
 * <code>Runner</code> is responsible for running benchmark tests
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class Runner implements TimeEventListener
{
    protected RunnerConfig config;
    protected int times = 0;
    protected int total = 0;
    protected int dumpVmStatsFrequency = 10;
    protected Runtime runtime = Runtime.getRuntime();
    protected long time = System.currentTimeMillis();
    protected  int counter;

    protected  NumberFormat formatter = NumberFormat.getInstance();

    public static void main(String[] args)
    {
        if(args.length==1 && args[0].equals("?")) {
            RunnerConfig.usage();
            System.exit(0);
        }
        try
        {
            Runner runner = new Runner(new RunnerConfig(args));
            runner.start();
        } catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public Runner(RunnerConfig config) throws Exception
    {
        this.config = config;
        System.out.println("Using config:");
        System.out.println(config.toString());
        loadConnectors();
    }

    protected void loadConnectors() throws Exception {
        List protocols = new ArrayList();
        if(config.getConnectorConfig()!=null) {
            Properties props = PropertiesUtils.loadProperties(config.getConnectorConfig());
            for (Iterator iterator = protocols.iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String key = entry.getKey().toString();
                String protocol = key.substring(0, key.indexOf("."));
                if(!protocols.contains(protocol)) {
                    protocols.add(protocol);
                    Map pp = new HashMap();
                    PropertiesUtils.getPropertiesWithPrefix(props, protocol, pp);
                    UMOConnector cnn = ConnectorFactory.getServiceDescriptor(protocol).createConnector(protocol);
                    cnn.setName(cnn.toString());
                    pp = PropertiesUtils.removeNamespaces(pp);
                    org.mule.util.BeanUtils.populateWithoutFail(cnn, pp, true);
                    MuleManager.getInstance().registerConnector(cnn);
                }
            }
        }
    }

    public void start() throws UMOException
    {
        Timer t = new Timer();
        EventTimerTask task = new EventTimerTask(this, "benchmark-loop");
        t.schedule(task, 2000, 1000);
        task.start();
        MuleManager.getInstance().start();
    }

    public void timeExpired(TimeEvent e)
    {
        int processed = resetCount();
        double average = 0;
        if (processed > 0)
        {
            total += processed;
            times++;
        }
        if (times > 0)
        {
            average = total / times;
        }

        long oldtime = time;
        time = System.currentTimeMillis();

        double diff = time - oldtime;
        if(processed==0) {
            System.out.println('.');
        } else {
            System.out.println(getClass().getName() + " Processed: " + processed + " messages this second. Average: " + average);
        }

        if ((times % dumpVmStatsFrequency) == 0 && times != 0)
        {
            System.out.println("Used memory: " + asMemoryString(runtime.totalMemory() - runtime.freeMemory())
                    + " Free memory: " + asMemoryString(runtime.freeMemory())
                    + " Total memory: " + asMemoryString(runtime.totalMemory())
                    + " Max memory: " + asMemoryString(runtime.maxMemory()));
        }
    }

    protected synchronized void count(int count)
    {
        counter += count;
    }

    protected synchronized int resetCount()
    {
        int count = counter;
        counter = 0;
        return count;
    }

    protected String asMemoryString(long value)
    {
        return formatter.format(value / 1024) + " kb";
    }
}

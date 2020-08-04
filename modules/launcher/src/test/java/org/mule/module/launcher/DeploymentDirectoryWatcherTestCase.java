/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.util.ObservableList;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DeploymentDirectoryWatcherTestCase
{

    private ArchiveDeployer<Domain> domainArchiveDeployer;
    private ArchiveDeployer<Application> applicationArchiveDeployer;
    private ReentrantLock deploymentLock;
    private ObservableList<Domain> domains;
    private ArrayList<Application> applicationArrayList;
    private ObservableList<Application> applications;
    private DeploymentDirectoryWatcher deploymentDirectoryWatcher;

    @Before
    public void setUp() throws Exception
    {
        domainArchiveDeployer = mock(ArchiveDeployer.class);
        applicationArchiveDeployer = mock(ArchiveDeployer.class);
        deploymentLock = mock(ReentrantLock.class);
        domains = new ObservableList<>(new ArrayList<Domain>());
        applicationArrayList = new ArrayList<>();
        applications = new ObservableList<>(applicationArrayList);

        deploymentDirectoryWatcher = new DeploymentDirectoryWatcher(domainArchiveDeployer, applicationArchiveDeployer, domains, applications, deploymentLock);

    }

    @Test
    public void testStopOneApplicationCancelsStart()
    {
        // Given a deploymentDirectoryWatcher with one application
        Application application = mock(Application.class);
        applicationArrayList.add(application);

        // When stopping
        deploymentDirectoryWatcher.stop();

        // Then cancel start is called for every application before requesting the lock
        InOrder inOrder = inOrder(application, deploymentLock);
        inOrder.verify(application).cancelStart();
        inOrder.verify(deploymentLock).lock();
    }

    @Test
    public void testStopThreeApplicationsCancelsStart()
    {
        // Given a deploymentDirectoryWatcher with one application
        Application application1 = mock(Application.class);
        applicationArrayList.add(application1);

        Application application2 = mock(Application.class);
        applicationArrayList.add(application2);

        Application application3 = mock(Application.class);
        applicationArrayList.add(application3);

        // When stopping
        deploymentDirectoryWatcher.stop();

        // Then cancel start is called for every application before requesting the lock
        InOrder inOrder = inOrder(application1, application2, application3, deploymentLock);
        inOrder.verify(application3, times(1)).cancelStart();
        inOrder.verify(application2, times(1)).cancelStart();
        inOrder.verify(application1, times(1)).cancelStart();
        inOrder.verify(deploymentLock).lock();
    }
}
/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.dao.impl;

import org.mule.example.geomail.dao.Sender;
import org.mule.example.geomail.dao.SenderDao;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;


@Transactional
public class SenderDaoImpl implements SenderDao
{

    private EntityManager entityManager;

    public Collection getSenders() {
        return getEntityManager().createQuery("SELECT sender FROM Sender sender").getResultList();
    }

    public Sender getSender(String senderId) {
        return getEntityManager().find(Sender.class, senderId);
    }

    public void addSender(Sender sender) {
        getEntityManager().persist(sender);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}

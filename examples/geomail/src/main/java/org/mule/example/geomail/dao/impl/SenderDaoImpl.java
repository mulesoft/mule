/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public Collection getSenders()
    {
        return getEntityManager().createQuery("SELECT sender FROM Sender sender").getResultList();
    }

    public Sender getSender(String senderId)
    {
        return getEntityManager().find(Sender.class, senderId);
    }

    public void addSender(Sender sender)
    {
        getEntityManager().persist(sender);
    }

    public EntityManager getEntityManager()
    {
        return entityManager;
    }

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }
}

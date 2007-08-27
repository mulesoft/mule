/*
 * $Id:InMemoryUserManager.java 7261 2007-06-27 02:23:03Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.AbstractUserManager;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.BaseUser;
import org.apache.ftpserver.usermanager.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.TransferRatePermission;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.WritePermission;
import org.apache.ftpserver.util.BaseProperties;
import org.apache.ftpserver.util.EncryptUtils;


/**
 * This class is needed to avoid creating unnesessary configuration files while running ftp transport tests.
 * Based on org.apache.ftpserver.usermanager.PropertiesUserManager
 */

public class InMemoryUserManager extends AbstractUserManager
{

    private static final String PREFIX = "ftpserver.user.";

    private BaseProperties userDataProp = new BaseProperties();
    private boolean isPasswordEncrypt = true;


    public InMemoryUserManager()
    {
        // this is just copied from default file ./res/user.gen
        userDataProp.setProperty("ftpserver.user.admin.userpassword", "21232F297A57A5A743894A0E4A801FC3");
        userDataProp.setProperty("ftpserver.user.admin.idletime", 0);
        userDataProp.setProperty("ftpserver.user.anonymous.enableflag", true);
        userDataProp.setProperty("ftpserver.user.anonymous.uploadrate", 4800);
        userDataProp.setProperty("ftpserver.user.admin.writepermission", true);
        userDataProp.setProperty("ftpserver.user.anonymous.userpassword", "D41D8CD98F00B204E9800998ECF8427E");
        userDataProp.setProperty("ftpserver.user.anonymous.maxloginperip", 2);
        userDataProp.setProperty("ftpserver.user.anonymous.idletime", 300);
        userDataProp.setProperty("ftpserver.user.anonymous.homedirectory", "./res/home");
        userDataProp.setProperty("ftpserver.user.admin.enableflag", true);
        userDataProp.setProperty("ftpserver.user.anonymous.downloadrate", 4800);
        userDataProp.setProperty("ftpserver.user.anonymous.maxloginnumber", 20);
        userDataProp.setProperty("ftpserver.user.admin.homedirectory", "./res/home");
        userDataProp.setProperty("ftpserver.user.anonymous.writepermission", false);

    }

    /**
     * Load user data.
     */
    public User getUserByName(String userName)
    {
        if (!doesExist(userName))
        {
            return null;
        }

        String baseKey = PREFIX + userName + '.';
        BaseUser user = new BaseUser();
        user.setName(userName);
        user.setEnabled(userDataProp.getBoolean(baseKey + ATTR_ENABLE, true));
        user.setHomeDirectory(userDataProp.getProperty(baseKey + ATTR_HOME, "/"));

        List authorities = new ArrayList();

        if (userDataProp.getBoolean(baseKey + ATTR_WRITE_PERM, false))
        {
            authorities.add(new WritePermission());
        }

        int maxLogin = userDataProp.getInteger(baseKey + ATTR_MAX_LOGIN_NUMBER, 0);
        int maxLoginPerIP = userDataProp.getInteger(baseKey + ATTR_MAX_LOGIN_PER_IP, 0);

        authorities.add(new ConcurrentLoginPermission(maxLogin, maxLoginPerIP));

        int uploadRate = userDataProp.getInteger(baseKey + ATTR_MAX_UPLOAD_RATE, 0);
        int downloadRate = userDataProp.getInteger(baseKey + ATTR_MAX_DOWNLOAD_RATE, 0);

        authorities.add(new TransferRatePermission(downloadRate, uploadRate));

        user.setAuthorities((Authority[]) authorities.toArray(new Authority[authorities.size()]));

        user.setMaxIdleTime(userDataProp.getInteger(baseKey + ATTR_MAX_IDLE_TIME, 0));

        return user;
    }

    /**
     * Get all user names.
     */
    public String[] getAllUserNames()
    {
        // get all user names
        String suffix = '.' + ATTR_HOME;
        ArrayList ulst = new ArrayList();
        Enumeration allKeys = userDataProp.propertyNames();
        int prefixlen = PREFIX.length();
        int suffixlen = suffix.length();
        while (allKeys.hasMoreElements())
        {
            String key = (String) allKeys.nextElement();
            if (key.endsWith(suffix))
            {
                String name = key.substring(prefixlen);
                int endIndex = name.length() - suffixlen;
                name = name.substring(0, endIndex);
                ulst.add(name);
            }
        }

        Collections.sort(ulst);
        return (String[]) ulst.toArray(new String[ulst.size()]);
    }

    /**
     * Delete an user. Removes all this user entries from the properties.
     * After removing the corresponding from the properties, save the data.
     */
    public void delete(String login)
    {
        // we don't need this
    }

    /**
     * Save user data. Store the properties.
     */
    public void save(User user)
    {
        // we don't need this
    }

    /**
     * User existance check
     */
    public boolean doesExist(String name)
    {
        String key = PREFIX + name + '.' + ATTR_HOME;
        return userDataProp.containsKey(key);
    }

    /**
     * User authenticate method
     */
    public synchronized User authenticate(Authentication authentication) throws AuthenticationFailedException
    {
        if (authentication instanceof UsernamePasswordAuthentication)
        {
            UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) authentication;

            String user = upauth.getUsername();
            String password = upauth.getPassword();

            if (user == null)
            {
                throw new AuthenticationFailedException("Authentication failed");
            }

            if (password == null)
            {
                password = "";
            }

            String passVal = userDataProp.getProperty(PREFIX + user + '.' + ATTR_PASSWORD);
            if (isPasswordEncrypt)
            {
                password = EncryptUtils.encryptMD5(password);
            }
            if (password.equals(passVal))
            {
                return getUserByName(user);
            } else
            {
                throw new AuthenticationFailedException("Authentication failed");
            }

        } else if (authentication instanceof AnonymousAuthentication)
        {
            if (doesExist("anonymous"))
            {
                return getUserByName("anonymous");
            } else
            {
                throw new AuthenticationFailedException("Authentication failed");
            }
        } else
        {
            throw new IllegalArgumentException("Authentication not supported by this user manager");
        }
    }
}

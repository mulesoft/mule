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
package org.mule.tools.mc4j;

import org.mc4j.console.bean.MBeanNode;
import org.mc4j.console.dashboard.components.AttributeTableComponent;

import javax.management.Notification;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NotificationTableComponent extends AttributeTableComponent {

    public void init() {
        mbeanNodes = new ArrayList();
        attributeNames = new ArrayList();
        attributeNames.add("Type");
        attributeNames.add("timestamp");
        attributeNames.add("Sequence");
        attributeNames.add("Message");
        super.init();
    }

    public void refresh() {

        if (this.tableModel instanceof AttributeTableModel)
            ((AttributeTableModel) this.tableModel).resetChangeList();

        int row = 0;
        List notifs = (List) mbeanNode.getAttributeNodeMap().get("NotificationList");
        for (Iterator iterator = notifs.iterator(); iterator.hasNext();) {
            Notification notification = (Notification) iterator.next();

            List attributes = new ArrayList();
            attributes.add(notification.getType());
            attributes.add(new Date(notification.getTimeStamp()));
            attributes.add(new Long(notification.getSequenceNumber()));
            attributes.add(notification.getMessage());
            int col = 0;
            for (Iterator iterator1 = attributes.iterator(); iterator1.hasNext();) {
                Object value = iterator1.next();
                if (!this.sorted) {
                    this.tableModel.setColumnClass(col, value.getClass());
                }
                if ((value != null) && !value.equals(this.tableModel.getValueAt(row, col))) {
                    this.tableModel.setValueAt(value, row, col);
                }
                col++;
            }
            if (!this.sorted)
                this.sorted = true;
            row++;

        }

        this.tableModel.fireTableDataChanged();
    }

    public MBeanNode getMBeanNode() {
        return mbeanNode;
    }

    public void setMBeanNode(MBeanNode node) {
        mbeanNode = node;
    }
}

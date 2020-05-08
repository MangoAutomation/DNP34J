/**
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.dnp34j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;
import br.org.scadabr.dnp34j.master.session.config.EthernetParameters;
import br.org.scadabr.dnp34j.master.session.database.DataElement;

/**
 *
 * @author Terry Packer
 */
public class TestReadFloat {

    public static void main(String[] args) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        EthernetParameters params = new EthernetParameters("10.8.0.100", 20000);
        DNPConfig configuration = new DNPConfig(params, 3, 4);
        configuration.setRequestTimeout(1500);
        configuration.setRequestRetries(2);

        DNPUser user = new DNPUser(configuration, null, null);
        user.init();
        try {
            for (int i = 0; i < 100; i++) {
                try {
                    user.sendSynch(user.buildReadAllEventDataMsg());
                    // Read float at index 2
                    List<DataElement> elements = user.getDatabase().read(2, 0x30);
                    if (elements != null) {
                        for (DataElement element : elements) {
                            long deviceTime = element.getTimestamp();
                            Number number = (Number)element.getValue();
                            double value = number.doubleValue();
                            System.out.println(value + " @ " + sdf.format(new Date(deviceTime)));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            user.stop();
        }

    }

}

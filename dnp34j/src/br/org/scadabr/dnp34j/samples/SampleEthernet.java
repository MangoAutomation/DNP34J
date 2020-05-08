package br.org.scadabr.dnp34j.samples;

import java.util.Calendar;
import java.util.List;

import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;
import br.org.scadabr.dnp34j.master.session.config.EthernetParameters;
import br.org.scadabr.dnp34j.master.session.database.DataElement;

public class SampleEthernet {
    private static DNPUser user;

    /**
     * @param args
     * @throws Exception
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        DNPConfig config = new DNPConfig(new EthernetParameters("150.162.165.190", 20000), 3, 4);

        // DNPConfig config = new DNPConfig(new SerialParameters("COM2", 9600), 3,
        // 4);
        user = new DNPUser(config, null, null);

        user.init();
        // 69775326259457
        Thread.sleep(1000);

        user.sendSynch(user.buildSetTimeAndDateMsg());
        for (int i = 0; i < 50000; i++) {
            try {
                doPoll();
                List<DataElement> binHist = user.getDatabase().readBinaryInputPoint(0);

                for (DataElement dataElement : binHist) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(dataElement.getTimestamp());
                    System.out.println("Bin 0 changed to " + dataElement.getValue() + " at "
                            + calendar.getTime());
                }
            } catch (Exception e) {
                System.out.println("Poll Falhou! " + e.getMessage());

            }

            Thread.sleep(2300);
        }
        user.stop();
        System.exit(0);
    }

    private static boolean reconnecting = false;
    private static int timeoutCount = 0;
    private static int timeoutsToReconnect = 3;
    private static int relativePollingPeriod = 10;
    private static int pollingCount = 0;

    public static void doPoll() throws Exception {
        System.out.println("Poll!");
        if (reconnecting) {
            System.out.println("Tentando reconectar!");
            timeoutCount = 0;
            try {
                try {
                    user.init();
                    reconnecting = false;
                } catch (Exception e) {
                    System.out.println("Reconnect falhou!");
                    terminate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (reconnectNeeded()) {
                reconnecting = true;
                terminate();
                throw new Exception("Link failed. Trying to reconnect.");
            } else {
                try {
                    if (pollingCount == 0) {
                        user.sendSynch(user.buildReadStaticDataMsg());
                        pollingCount++;
                    } else {
                        user.sendSynch(user.buildReadAllEventDataMsg());
                        pollingCount++;
                        if (pollingCount > relativePollingPeriod)
                            pollingCount = 0;
                    }
                    timeoutCount = 0;
                } catch (Exception e) {
                    System.out.println("[DNP3Master] polling failed!");
                    timeoutCount++;
                }

            }
        }

        Thread.sleep(150);
    }

    private static boolean reconnectNeeded() {
        if (timeoutCount >= timeoutsToReconnect) {
            return true;
        }
        return false;
    }

    public static void terminate() throws Exception {
        user.stop();
    }

}

package test;

import java.util.Date;
import java.util.List;

import br.org.scadabr.dnp34j.master.session.DNPUser;
import br.org.scadabr.dnp34j.master.session.config.DNPConfig;
import br.org.scadabr.dnp34j.master.session.config.EthernetParameters;
import br.org.scadabr.dnp34j.master.session.database.DataElement;

public class Test {
    static DNPUser user;

    public static void main(String[] args) throws Exception {
        EthernetParameters params = new EthernetParameters("mangoautomation.net", 20000);
        DNPConfig configuration = new DNPConfig(params, 11, 0);
        user = new DNPUser(configuration, null);
        user.init();

        try {
            for (int i = 0; i < 10; i++) {
                try {
                    System.out.println(" ");
                    System.out.println("Poll: " + i);
                    doStaticPoll();
                    //                doRBEPoll();
                    dumpValues();
                    System.out.println(" ");
                    Thread.sleep(1000);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //            doStaticPoll();
            //            dumpValues();
        }
        finally {
            user.stop();
        }
    }

    static void doStaticPoll() throws Exception {
        user.sendSynch(user.buildReadStaticDataMsg());
    }

    static void doRBEPoll() throws Exception {
        user.sendSynch(user.buildReadAllEventDataMsg());
    }

    static void dumpValues() {
        dumpValue(0x30, 0);
        dumpValue(0x30, 2);
        dumpValue(0x1, 1);
    }

    static void dumpValue(int group, int index) {
        List<DataElement> es = getElement((byte) group, index);
        if (es == null)
            System.out.println("null");
        else {
            for (DataElement e : es)
                System.out.println(group + "/" + index + ": " + e.getValue() + " @ " + new Date(e.getTimestamp()));
        }
    }

    static List<DataElement> getElement(byte group, int index) {
        return user.getDatabase().read(index, group);
    }

    //    private static void printPoints() throws Exception {
    //        System.out.println("*** BinaryInputPoints");
    //        printPoints(master.getBinaryInputPoints());
    //        System.out.println("*** BinaryOutputPoints");
    //        printPoints(master.getBinaryOutputPoints());
    //        System.out.println("*** CounterInputPoints");
    //        printPoints(master.getCounterInputPoints());
    //        System.out.println("*** AnalogInputPoints");
    //        printPoints(master.getAnalogInputPoints());
    //        System.out.println("*** AnalogOutputPoints");
    //        printPoints(master.getAnalogOutputPoints());
    //        System.out.println();
    //
    //        //        for (int i = 0; i < 50; i++) {
    //        //            if (i % 5 == 0) {
    //        //                System.out.println("");
    //        //                System.out.print(master.getElement((byte) 0, i).getValue() + " / ");
    //        //            }
    //        //            else
    //        //                System.out.print(master.getElement((byte) 0, i).getValue() + " / ");
    //        //        }
    //    }
    //
    //    static void printPoints(HashMap<Integer, DataBuffer> map) {
    //        for (Entry<Integer, DataBuffer> e : map.entrySet()) {
    //            System.out.println("   " + e.getKey());
    //            for (DataElement elem : e.getValue().readAndPop())
    //                System.out.println("      " + elem);
    //        }
    //    }
}

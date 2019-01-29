package TEMeasure.GUI;

import JISA.GUI.Grid;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;

public class TCConfigTab extends Grid {


    public  TCConfig      stage;
    public  TCConfig      base;
    public  TCConfig      tc3;
    public  TCConfig      tc4;
    private ConnectionTab connectionTab;

    public TCConfigTab(ConnectionTab connectionTab) {
        super("T-Controller Config");
        setNumColumns(2);
        setGrowth(false, false);
        this.connectionTab = connectionTab;

        stage = new TCConfig("Stage Temperature", connectionTab.tcs);
        base = new TCConfig("Base Temperature", connectionTab.tcs);
        tc3 = new TCConfig("T-Controller 3", connectionTab.tcs);
        tc4 = new TCConfig("T-Controller 4", connectionTab.tcs);

        add(stage);
        add(base);
        add(tc3);
        add(tc4);

    }
}

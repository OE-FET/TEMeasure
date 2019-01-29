package TEMeasure.GUI;

import JISA.GUI.Grid;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;

public class TCConfigTab extends Grid {


    public TCConfig stage;
    public TCConfig shield;
    public TCConfig fStage;
    public TCConfig sStage;

    public TCConfigTab(ConnectionTab connectionTab) {
        super("T-Controller Config");
        setNumColumns(2);
        setGrowth(false, false);

        stage = new TCConfig("Sample", connectionTab.tcs);
        shield = new TCConfig("Radiation Shield", connectionTab.tcs);
        fStage = new TCConfig("First Stage", connectionTab.tcs);
        sStage = new TCConfig("Second Stage", connectionTab.tcs);

        add(stage);
        add(shield);
        add(fStage);
        add(sStage);

    }
}

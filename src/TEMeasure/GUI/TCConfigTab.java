package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Grid;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;

public class TCConfigTab extends Grid {


    public TCConfig stage;
    public TCConfig shield;
    public TCConfig fStage;
    public TCConfig sStage;

    public TCConfigTab(ConnectionTab connectionTab, ConfigStore configStore) {
        super("T-Controller Config");
        setNumColumns(2);
        setGrowth(true, false);

        stage = new TCConfig("Sample", "sTC", configStore, connectionTab.tcs);
        shield = new TCConfig("Radiation Shield", "rTC", configStore, connectionTab.tcs);
        fStage = new TCConfig("First Stage", "fsTC", configStore, connectionTab.tcs);
        sStage = new TCConfig("Second Stage", "ssTC", configStore, connectionTab.tcs);

        add(stage);
        add(shield);
        add(fStage);
        add(sStage);

    }
}

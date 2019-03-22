package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Grid;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;

public class TCConfigTab extends Grid {


    TCConfig stage;
    TCConfig shield;
    TCConfig fStage;
    TCConfig sStage;

    public TCConfigTab(MainWindow mainWindow) {
        super("T-Controller Config");
        setNumColumns(2);
        setGrowth(true, false);

        stage = new TCConfig("Sample", "sTC", mainWindow.configStore, mainWindow.connectionTab);
        shield = new TCConfig("Radiation Shield", "rTC", mainWindow.configStore, mainWindow.connectionTab);
        fStage = new TCConfig("First Stage", "fsTC", mainWindow.configStore, mainWindow.connectionTab);
        sStage = new TCConfig("Second Stage", "ssTC", mainWindow.configStore, mainWindow.connectionTab);

        add(stage);
        add(shield);
        add(fStage);
        add(sStage);

    }
}

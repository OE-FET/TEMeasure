package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Grid;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;
import JISA.GUI.TMeterConfig;

public class TCConfigTab extends Grid {


    TCConfig stage;
    TCConfig shield;
    TCConfig fStage;
    TCConfig sStage;

    TMeterConfig sampleSense;
    TMeterConfig radSense;
    TMeterConfig armSense;
    TMeterConfig refSense;
    TMeterConfig fStageSense;
    TMeterConfig sStageSense;

    public TCConfigTab(MainWindow mainWindow) {
        super("T-Controller Config");
        setNumColumns(2);
        setGrowth(true, false);

        stage = new TCConfig("Sample Controller", "sTC", mainWindow.configStore, mainWindow.connectionTab);
        shield = new TCConfig("Radiation Shield Controller", "rTC", mainWindow.configStore, mainWindow.connectionTab);
        fStage = new TCConfig("First Stage Controller", "fsTC", mainWindow.configStore, mainWindow.connectionTab);
        sStage = new TCConfig("Second Stage Controller", "ssTC", mainWindow.configStore, mainWindow.connectionTab);


        sampleSense = new TMeterConfig("Sample Sensor", "sampleSense", mainWindow.configStore, mainWindow.connectionTab);
        radSense    = new TMeterConfig("Radiation Shield Sensor", "radSense", mainWindow.configStore, mainWindow.connectionTab);
        armSense    = new TMeterConfig("Arm Sensor", "armSense", mainWindow.configStore, mainWindow.connectionTab);
        refSense    = new TMeterConfig("Reference Sensor", "refSense", mainWindow.configStore, mainWindow.connectionTab);
        fStageSense = new TMeterConfig("First Stage Sensor", "fStageSense", mainWindow.configStore, mainWindow.connectionTab);
        sStageSense = new TMeterConfig("Second Stage Sensor", "sStageSense", mainWindow.configStore, mainWindow.connectionTab);

        add(stage);
        add(shield);
        add(fStage);
        add(sStage);

        addAll(sampleSense, radSense, armSense, refSense, fStageSense, sStageSense);

    }
}

package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Grid;
import JISA.GUI.InstrumentConfig;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;

public class SMUConfigTab extends Grid {

    SMUConfig heaterSMU;
    SMUConfig rtSMU;
    SMUConfig tvSMU;
    SMUConfig hotGateSMU;
    SMUConfig coldGateSMU;

    public SMUConfigTab(MainWindow mainWindow) {
        super("SMU Config");
        setNumColumns(2);
        setGrowth(true, false);

        heaterSMU   = new SMUConfig("Heater SMU", "heaterSMU", mainWindow.configStore, mainWindow.connectionTab);
        rtSMU       = new SMUConfig("RT SMU", "rtSMU", mainWindow.configStore, mainWindow.connectionTab);
        tvSMU       = new SMUConfig("Thermo-Voltage SMU", "tvSMU", mainWindow.configStore, mainWindow.connectionTab);
        hotGateSMU  = new SMUConfig("Hot-Gate SMU", "hgSMU", mainWindow.configStore, mainWindow.connectionTab);
        coldGateSMU = new SMUConfig("Cold-Gate SMU", "cgSMU", mainWindow.configStore, mainWindow.connectionTab);

        add(heaterSMU);
        add(rtSMU);
        add(tvSMU);
        add(hotGateSMU);
        add(coldGateSMU);

    }
}

package temeasure.gui;

import jisa.gui.*;

public class SMUConfigTab extends Grid {

    SMUConfig    heaterSMU;
    SMUConfig    rtSMU;
    VMeterConfig tvSMU;
    SMUConfig    hotGateSMU;
    SMUConfig    coldGateSMU;

    public SMUConfigTab(MainWindow mainWindow) {
        super("SMU Config");
        setNumColumns(2);
        setGrowth(true, false);

        heaterSMU   = new SMUConfig("Heater SMU", "heaterSMU", mainWindow.configStore, mainWindow.connectionTab);
        rtSMU       = new SMUConfig("RT SMU", "rtSMU", mainWindow.configStore, mainWindow.connectionTab);
        tvSMU       = new VMeterConfig("Thermo-Voltage Meter", "tvVM", mainWindow.configStore, mainWindow.connectionTab);
        hotGateSMU  = new SMUConfig("Hot-Gate SMU", "hgSMU", mainWindow.configStore, mainWindow.connectionTab);
        coldGateSMU = new SMUConfig("Cold-Gate SMU", "cgSMU", mainWindow.configStore, mainWindow.connectionTab);

        add(heaterSMU);
        add(rtSMU);
        add(tvSMU);
        add(hotGateSMU);
        add(coldGateSMU);

    }

}

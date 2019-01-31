package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.GUI.Grid;
import JISA.GUI.InstrumentConfig;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;

public class SMUConfigTab extends Grid {

    public  SMUConfig     heaterSMU;
    public  SMUConfig     rtSMU;
    public  SMUConfig     tvSMU;
    public  SMUConfig     hotGateSMU;
    public  SMUConfig     coldGateSMU;
    private ConnectionTab connectionTab;

    public SMUConfigTab(ConnectionTab connectionTab, ConfigStore configStore) {
        super("SMU Config");
        setNumColumns(2);
        setGrowth(true, false);
        this.connectionTab = connectionTab;

        heaterSMU = new SMUConfig("Heater SMU", "heaterSMU", configStore, connectionTab.smus);
        rtSMU = new SMUConfig("RT SMU", "rtSMU", configStore, connectionTab.smus);
        tvSMU = new SMUConfig("Thermo-Voltage SMU", "tvSMU", configStore, connectionTab.smus);
        hotGateSMU = new SMUConfig("Hot-Gate SMU", "hgSMU", configStore, connectionTab.smus);
        coldGateSMU = new SMUConfig("Cold-Gate SMU", "cgSMU", configStore, connectionTab.smus);

        add(heaterSMU);
        add(rtSMU);
        add(tvSMU);
        add(hotGateSMU);
        add(coldGateSMU);

    }
}

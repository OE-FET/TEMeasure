package TEMeasure.GUI;

import JISA.GUI.Grid;
import JISA.GUI.InstrumentConfig;
import JISA.GUI.SMUConfig;
import JISA.GUI.TCConfig;

public class SMUConfigTab extends Grid {

    public  SMUConfig     heaterSMU;
    public  SMUConfig     rtSMU;
    public  SMUConfig     tvSMU;
    public  SMUConfig     gateSMU;
    private ConnectionTab connectionTab;

    public SMUConfigTab(ConnectionTab connectionTab) {
        super("SMU Config");
        setNumColumns(2);
        setGrowth(false, false);
        this.connectionTab = connectionTab;

        heaterSMU = new SMUConfig("Heater SMU", connectionTab.smus);
        rtSMU = new SMUConfig("RT SMU", connectionTab.smus);
        tvSMU = new SMUConfig("Thermo-Voltage SMU", connectionTab.smus);
        gateSMU = new SMUConfig("Gate SMU", connectionTab.smus);

        add(heaterSMU);
        add(rtSMU);
        add(tvSMU);
        add(gateSMU);

    }
}

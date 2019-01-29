package TEMeasure.GUI;

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

    public SMUConfigTab(ConnectionTab connectionTab) {
        super("SMU Config");
        setNumColumns(2);
        setGrowth(false, false);
        this.connectionTab = connectionTab;

        heaterSMU = new SMUConfig("Heater SMU", connectionTab.smus);
        rtSMU = new SMUConfig("RT SMU", connectionTab.smus);
        tvSMU = new SMUConfig("Thermo-Voltage SMU", connectionTab.smus);
        hotGateSMU = new SMUConfig("Hot-Gate SMU", connectionTab.smus);
        coldGateSMU = new SMUConfig("Cold-Gate SMU", connectionTab.smus);

        add(heaterSMU);
        add(rtSMU);
        add(tvSMU);
        add(hotGateSMU);
        add(coldGateSMU);

    }
}

package temeasure.gui;

import jisa.devices.SMU;
import jisa.devices.TC;
import jisa.devices.VMeter;
import jisa.gui.ConfigGrid;
import jisa.gui.InstrumentConfig;

public class ConnectionTab extends ConfigGrid {

    private InstrumentConfig<SMU>    smu1;
    private InstrumentConfig<SMU>    smu2;
    private InstrumentConfig<SMU>    smu3;
    private InstrumentConfig<SMU>    smu4;
    private InstrumentConfig<VMeter> vMeter;
    private InstrumentConfig<TC>     tc1;
    private InstrumentConfig<TC>     tc2;
    private InstrumentConfig<TC>     tc3;
    private InstrumentConfig<TC>     tc4;

    @SuppressWarnings("unchecked")
    public ConnectionTab(MainWindow mainWindow) {
        super("Connections", mainWindow.configStore);
        setNumColumns(3);
        setGrowth(true, false);

        smu1   = addInstrument("SMU 1", SMU.class);
        smu2   = addInstrument("SMU 2", SMU.class);
        smu3   = addInstrument("SMU 3", SMU.class);
        smu4   = addInstrument("SMU 4", SMU.class);
        vMeter = addInstrument("Voltmeter", VMeter.class);
        tc1    = addInstrument("Temperature Controller 1", TC.class);
        tc2    = addInstrument("Temperature Controller 2", TC.class);
        tc3    = addInstrument("Temperature Controller 3", TC.class);
        tc4    = addInstrument("Temperature Controller 4", TC.class);

        connectAll();

    }

}

package TEMeasure.GUI;

import JISA.Control.ConfigStore;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.GUI.ConfigGrid;
import JISA.GUI.InstrumentConfig;

public class ConnectionTab extends ConfigGrid {

    private InstrumentConfig<SMU>         smu1;
    private InstrumentConfig<SMU>         smu2;
    private InstrumentConfig<SMU>         smu3;
    private InstrumentConfig<SMU>         smu4;
    private InstrumentConfig<TC> tc1;
    private InstrumentConfig<TC> tc2;
    private InstrumentConfig<TC> tc3;
    private InstrumentConfig<TC> tc4;

    public InstrumentConfig<SMU>[]         smus;
    public InstrumentConfig<TC>[] tcs;

    @SuppressWarnings("unchecked")
    public ConnectionTab(ConfigStore configStore) {
        super("Connections", configStore);
        setNumColumns(2);
        setGrowth(true, false);

        smu1 = addInstrument("SMU 1", SMU.class);
        smu2 = addInstrument("SMU 2", SMU.class);
        smu3 = addInstrument("SMU 3", SMU.class);
        smu4 = addInstrument("SMU 4", SMU.class);
        tc1 = addInstrument("Temperature Controller 1", TC.class);
        tc2 = addInstrument("Temperature Controller 2", TC.class);
        tc3 = addInstrument("Temperature Controller 3", TC.class);
        tc4 = addInstrument("Temperature Controller 4", TC.class);

        smus = new InstrumentConfig[]{smu1, smu2, smu3, smu4};
        tcs  = new InstrumentConfig[]{tc1, tc2, tc3, tc4};

        connectAll();

    }
}

package TEMeasure.GUI;

import JISA.Control.Field;
import JISA.Devices.DeviceException;
import JISA.Devices.SMU;
import JISA.Devices.TController;
import JISA.Experiment.ResultTable;
import JISA.GUI.*;
import TEMeasure.Measurement.GatedTEM;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.LinkedList;

public class GatedTEMTab extends Grid {

    private final SMUConfig heaterSMU;
    private final SMUConfig gateSMU;
    private final SMUConfig tvSMU;
    private final TCConfig  stageTC;
    private final Fields    gateParams   = new Fields("Gate Parameters");
    private final Fields    heaterParams = new Fields("Heater Parameters");
    private final Fields    otherParams  = new Fields("Other Parameters");

    private final Field<Double>  gateStart;
    private final Field<Double>  gateStop;
    private final Field<Integer> gateSteps;
    private final Field<Double>  gateTime;

    private final Field<Double>  heaterStart;
    private final Field<Double>  heaterStop;
    private final Field<Integer> heaterSteps;
    private final Field<Double>  heaterTime;

    private final Field<Double> intTime;
    private final Field<String> outputFile;

    private final Plot heaterPlot  = new Plot("Heater Power", "Measurement No.", "Heater Power [W]");
    private final Plot gatePlot    = new Plot("Gate Voltage", "Measurement No.", "Gate Voltage [V]");
    private final Plot thermalPlot = new Plot("Thermo-Voltage", "Measurement No.", "Thermo-Voltage [V]");
    private final Plot tpPlot      = new Plot("TV vs Power", "Heater Power [W]", "Thermo-Voltage [V]");

    private GatedTEM measurement = null;

    public GatedTEMTab(SMUConfig heaterSMU, SMUConfig gateSMU, SMUConfig tvSMU, TCConfig stageTC) {

        super("Gated TE Measurement");
        this.heaterSMU = heaterSMU;
        this.gateSMU = gateSMU;
        this.tvSMU = tvSMU;
        this.stageTC = stageTC;

        setNumColumns(3);
        setGrowth(true, false);

        // Set-up gate parameters panel
        gateStart = gateParams.addDoubleField("Start Gate [V]");
        gateStop = gateParams.addDoubleField("Stop Gate [V]");
        gateSteps = gateParams.addIntegerField("No. Steps");
        gateParams.addSeparator();
        gateTime = gateParams.addDoubleField("Hold Time [s]");
        add(gateParams);

        // Set-up heater parameters panel
        heaterStart = heaterParams.addDoubleField("Start Heater [V]");
        heaterStop = heaterParams.addDoubleField("Stop Heater [V]");
        heaterSteps = heaterParams.addIntegerField("No. Steps");
        heaterParams.addSeparator();
        heaterTime = heaterParams.addDoubleField("Hold Time [s]");
        add(heaterParams);

        // Set-up other parameters panel
        intTime = otherParams.addDoubleField("Integration Time [s]");
        outputFile = otherParams.addFileSave("Output File");
        add(otherParams);

        add(heaterPlot);
        add(gatePlot);
        add(thermalPlot);
        add(tpPlot);

        heaterPlot.showLegend(false);
        gatePlot.showLegend(false);
        thermalPlot.showLegend(false);

        addToolbarButton("Start", this::run);
        addToolbarButton("Stop", this::stop);

        fillDefaults();

    }

    private void fillDefaults() {

        heaterStart.set(0.0);
        heaterStop.set(5.0);
        heaterSteps.set(6);
        heaterTime.set(10.0);

        gateStart.set(-40.0);
        gateStop.set(0.0);
        gateSteps.set(9);
        gateTime.set(20.0);

        intTime.set(10.0 / 50.0);

    }

    public void run() throws IOException, DeviceException {

        try {

            SMU                thermoVoltage = tvSMU.getSMU();
            SMU                gateVoltage   = gateSMU.getSMU();
            SMU                heaterVoltage = heaterSMU.getSMU();
            TController        stageTemp     = stageTC.getTController();
            LinkedList<String> errors        = new LinkedList<>();

            if (thermoVoltage == null) {
                errors.add("Thermo-Voltage SMU is not configured.");
            }

            if (gateVoltage == null) {
                errors.add("Gate SMU is not configured.");
            }

            if (heaterVoltage == null) {
                errors.add("Heater SMU is not configured.");
            }

            if (stageTemp == null) {
                errors.add("Stage T-Controller is not configured.");
            }

            if (outputFile.get().trim().equals("")) {
                errors.add("No output file specified.");
            }

            if (!errors.isEmpty()) {
                GUI.errorAlert("Error", "Error Starting Measurement", String.join("\n\n", errors), 600);
                return;
            }

            measurement = new GatedTEM(thermoVoltage, gateVoltage, heaterVoltage, stageTemp);

            measurement.configureGate(gateStart.get(), gateStop.get(), gateSteps.get())
                       .configureHeater(heaterStart.get(), heaterStop.get(), heaterSteps.get())
                       .configureTiming(gateTime.get(), heaterTime.get(), intTime.get());

            ResultTable results = measurement.newResults(outputFile.get());

            heaterPlot.clear();
            heaterPlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_HEATER_POWER, "Heater", Color.TEAL);

            gatePlot.clear();
            gatePlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_GATE_VOLTAGE, "Gate", Color.CYAN);

            thermalPlot.clear();
            gatePlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_THERMO_VOLTAGE, "Thermo-Voltage", Color.ORANGE);

            tpPlot.clear();
            tpPlot.watchList(results, GatedTEM.COL_HEATER_POWER, GatedTEM.COL_THERMO_VOLTAGE, GatedTEM.COL_GATE_SET_VOLTAGE);

            measurement.performMeasurement();

        } catch (Exception e) {
            GUI.errorAlert("Error", "Exception Encountered", e.getMessage());
        }

    }

    public void stop() {
        if (measurement != null) {
            measurement.stop();
        }
    }

    public boolean isRunning() {
        return measurement != null && measurement.isRunning();
    }

}

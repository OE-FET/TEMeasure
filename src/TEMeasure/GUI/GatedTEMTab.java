package TEMeasure.GUI;

import JISA.Control.Field;
import JISA.Devices.DeviceException;
import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Experiment.ResultTable;
import JISA.GUI.*;
import TEMeasure.Measurement.GatedTEM;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.LinkedList;

public class GatedTEMTab extends Grid {

    private final SMUConfig heaterSMU;
    private final SMUConfig hotGateSMU;
    private final SMUConfig coldGateSMU;
    private final SMUConfig tvSMU;
    private final TCConfig  stageTC;
    private final Fields    gateParams   = new Fields("Gate");
    private final Fields    heaterParams = new Fields("Heater");
    private final Fields    otherParams  = new Fields("Other");

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

    private final Plot  heaterPlot  = new Plot("Heater Power", "Measurement No.", "Heater Power [uW]");
    private final Plot  gatePlot    = new Plot("Gate Voltage", "Measurement No.", "Gate Voltage [V]");
    private final Plot  thermalPlot = new Plot("Thermo-Voltage", "Measurement No.", "Thermo-Voltage [uV]");
    private final Plot  tpPlot      = new Plot("TV vs Power", "Heater Power [uW]", "Thermo-Voltage [uV]");
    private final Table table      = new Table("Table of Results");

    private Thread runner;

    private GatedTEM measurement = null;

    public GatedTEMTab(SMUConfig heaterSMU, SMUConfig hotGateSMU, SMUConfig coldGateSMU, SMUConfig tvSMU, TCConfig stageTC) {

        super("Gated TE Measurement");
        this.heaterSMU = heaterSMU;
        this.hotGateSMU = hotGateSMU;
        this.coldGateSMU = coldGateSMU;
        this.tvSMU = tvSMU;
        this.stageTC = stageTC;

        setNumColumns(1);
        setGrowth(true, false);

        // Set-up gate parameters panel
        gateStart = gateParams.addDoubleField("Start Gate [V]");
        gateStop = gateParams.addDoubleField("Stop Gate [V]");
        gateSteps = gateParams.addIntegerField("No. Steps");
        gateParams.addSeparator();
        gateTime = gateParams.addDoubleField("Hold Time [s]");

        // Set-up heater parameters panel
        heaterStart = heaterParams.addDoubleField("Start Heater [V]");
        heaterStop = heaterParams.addDoubleField("Stop Heater [V]");
        heaterSteps = heaterParams.addIntegerField("No. Steps");
        heaterParams.addSeparator();
        heaterTime = heaterParams.addDoubleField("Hold Time [s]");

        // Set-up other parameters panel
        intTime = otherParams.addDoubleField("Integration Time [s]");
        outputFile = otherParams.addFileSave("Output File");

        Grid topGrid = new Grid("", gateParams, heaterParams, otherParams);
        Grid bottomGrid = new Grid("", heaterPlot, gatePlot, thermalPlot, tpPlot);
        bottomGrid.setNumColumns(2);;

        add(topGrid);
        add(bottomGrid);
        add(new Grid("", table));

        heaterPlot.showLegend(false);
        gatePlot.showLegend(true);
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

    private void disableInputs(boolean disable) {

        gateStart.setDisabled(disable);
        gateStop.setDisabled(disable);
        gateSteps.setDisabled(disable);
        gateTime.setDisabled(disable);
        heaterStart.setDisabled(disable);
        heaterStop.setDisabled(disable);
        heaterSteps.setDisabled(disable);
        heaterTime.setDisabled(disable);
        intTime.setDisabled(disable);
        outputFile.setDisabled(disable);

    }

    public void run() throws IOException, DeviceException {

        try {

            runner = Thread.currentThread();

            disableInputs(true);

            SMU                thermoVoltage   = tvSMU.getSMU();
            SMU                hotGateVoltage  = hotGateSMU.getSMU();
            SMU                coldGateVoltage = coldGateSMU.getSMU();
            SMU                heaterVoltage   = heaterSMU.getSMU();
            TC                 stageTemp       = stageTC.getTController();
            LinkedList<String> errors          = new LinkedList<>();

            if (thermoVoltage == null) {
                errors.add("Thermo-Voltage SMU is not configured.");
            }

            if (hotGateVoltage == null) {
                errors.add("Hot-Gate SMU is not configured.");
            }

            if (coldGateVoltage == null) {
                errors.add("Cold-Gate SMU is not configured.");
            }

            if (heaterVoltage == null) {
                errors.add("Heater SMU is not configured.");
            }

            if (stageTemp == null) {
                errors.add("Sample T-Controller is not configured.");
            }

            if (outputFile.get().trim().equals("")) {
                errors.add("No output file specified.");
            }

            if (!errors.isEmpty()) {
                GUI.errorAlert("Error", "Error Starting Measurement", String.join("\n\n", errors), 600);
                return;
            }

            measurement = new GatedTEM(thermoVoltage, hotGateVoltage, coldGateVoltage, heaterVoltage, stageTemp);

            measurement.configureGate(gateStart.get(), gateStop.get(), gateSteps.get())
                       .configureHeater(heaterStart.get(), heaterStop.get(), heaterSteps.get())
                       .configureTiming(gateTime.get(), heaterTime.get(), intTime.get());

            ResultTable results = measurement.newResults(outputFile.get());

            heaterPlot.clear();
            heaterPlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_HEATER_POWER, "Heater", Color.TEAL);

            gatePlot.clear();
            gatePlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_GATE_VOLTAGE, GatedTEM.COL_GATE_CONFIG, 0.0, "Hot-Gate", Color.ORANGERED);
            gatePlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_GATE_VOLTAGE, GatedTEM.COL_GATE_CONFIG, 1.0, "Cold-Gate", Color.CORNFLOWERBLUE);


            thermalPlot.clear();
            thermalPlot.watchList(results, GatedTEM.COL_NUMBER, GatedTEM.COL_THERMO_VOLTAGE, "Thermo-Voltage", Color.PURPLE);

            tpPlot.clear();
            tpPlot.watchList(results, GatedTEM.COL_HEATER_POWER, GatedTEM.COL_THERMO_VOLTAGE, GatedTEM.COL_GATE_SET_VOLTAGE, GatedTEM.COL_GATE_CONFIG, 0.0);
            tpPlot.watchList(results, GatedTEM.COL_HEATER_POWER, GatedTEM.COL_THERMO_VOLTAGE, GatedTEM.COL_GATE_SET_VOLTAGE, GatedTEM.COL_GATE_CONFIG, 1.0);

            table.clear();
            table.watchList(results);

            measurement.performMeasurement();

            if (measurement.wasStopped()) {
                GUI.infoAlert("Stopped", "Measurement Stopped", "The measurement was stopped.");
            } else {
                GUI.infoAlert("Complete", "Measurement Completed", "The measurement completed without error.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            GUI.errorAlert("Error", "Exception Encountered", e.getMessage());
        } finally {
            disableInputs(false);
        }

    }

    public void stop() {
        if (measurement != null) {
            measurement.stop();
            runner.interrupt();
        }
    }

    public boolean isRunning() {
        return measurement != null && measurement.isRunning();
    }

}

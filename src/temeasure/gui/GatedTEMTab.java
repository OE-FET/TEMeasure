package temeasure.gui;

import jisa.control.Field;
import jisa.devices.SMU;
import jisa.devices.TC;
import jisa.devices.VMeter;
import jisa.experiment.ResultTable;
import jisa.gui.*;
import temeasure.measurement.GatedTEM;

import java.util.LinkedList;

@SuppressWarnings("Duplicates")
public class GatedTEMTab extends Grid {

    private final MainWindow mainWindow;
    private final Fields     gateParams   = new Fields("Gate");
    private final Fields     heaterParams = new Fields("Heater");
    private final Fields     otherParams  = new Fields("Other");

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

    private final Plot  heaterPlot  = new Plot("Heater Power", "Measurement No.", "Heater Power [W]");
    private final Plot  gatePlot    = new Plot("Gate Voltage", "Measurement No.", "Gate Voltage [V]");
    private final Plot  thermalPlot = new Plot("Thermo-Voltage", "Measurement No.", "Thermo-Voltage [V]");
    private final Plot  tpPlot      = new Plot("TV vs Power", "Heater Power [W]", "Thermo-Voltage [V]");
    private final Table table       = new Table("Table of Results");

    private GatedTEM measurement = null;

    public GatedTEMTab(MainWindow mainWindow) {

        super("Gated TE Measurement", 1);
        setGrowth(true, false);

        this.mainWindow = mainWindow;

        // Set-up gate parameters panel
        gateStart = gateParams.addDoubleField("Start Gate [V]", -7.0);
        gateStop  = gateParams.addDoubleField("Stop Gate [V]", -2.0);
        gateSteps = gateParams.addIntegerField("No. Steps", 11);
        gateParams.addSeparator();
        gateTime = gateParams.addDoubleField("Hold Time [s]", 20.0);

        // Set-up heater parameters panel
        heaterStart = heaterParams.addDoubleField("Start Heater [V]", 0.0);
        heaterStop  = heaterParams.addDoubleField("Stop Heater [V]", -5.0);
        heaterSteps = heaterParams.addIntegerField("No. Steps", 11);
        heaterParams.addSeparator();
        heaterTime = heaterParams.addDoubleField("Hold Time [s]", 30.0);

        // Set-up other parameters panel
        intTime    = otherParams.addDoubleField("Integration Time [s]", 200e-3);
        outputFile = otherParams.addFileSave("Output File", "");

        gateParams.loadFromConfig("tem-gate-params", mainWindow.configStore);
        heaterParams.loadFromConfig("tem-heater-params", mainWindow.configStore);
        otherParams.loadFromConfig("tem-other-params", mainWindow.configStore);

        Grid topGrid    = new Grid(3, gateParams, heaterParams, otherParams);
        Grid bottomGrid = new Grid(2, heaterPlot, gatePlot, thermalPlot, tpPlot);

        add(topGrid);
        add(bottomGrid);
        add(new Grid(1, table));

        heaterPlot.showLegend(false);
        gatePlot.showLegend(true);
        thermalPlot.showLegend(false);

        addToolbarButton("Start", this::run);
        addToolbarButton("Stop", this::stop);

    }

    private void disableInputs(boolean disable) {

        gateParams.setFieldsDisabled(disable);
        heaterParams.setFieldsDisabled(disable);
        otherParams.setFieldsDisabled(disable);

    }

    /**
     * Checks if all instruments are present and, if so, runs the measurement
     */
    private void run() {

        // Make sure nothing else is running
        if (mainWindow.isRunning()) {
            GUI.errorAlert("Error", "Already Running", "Another measurement is already running!");
            return;
        }

        ResultTable results = null;

        try {

            // Disabled all the text-boxes etc
            disableInputs(true);

            // Get the instruments that have been configured on the config tabs
            VMeter             thermoVoltage   = mainWindow.smuConfigTab.tvSMU.get();
            SMU                hotGateVoltage  = mainWindow.smuConfigTab.hotGateSMU.get();
            SMU                coldGateVoltage = mainWindow.smuConfigTab.coldGateSMU.get();
            SMU                heaterVoltage   = mainWindow.smuConfigTab.heaterSMU.get();
            TC                 stageTemp       = mainWindow.tcConfigTab.stage.get();
            LinkedList<String> errors          = new LinkedList<>();

            // Check that everything is present and configured
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

            // Create a new measurement object using our instruments
            measurement = new GatedTEM(thermoVoltage, hotGateVoltage, coldGateVoltage, heaterVoltage, stageTemp);

            // Configure experiment parameters using values in fields
            measurement.configureGate(gateStart.get(), gateStop.get(), gateSteps.get())
                       .configureHeater(heaterStart.get(), heaterStop.get(), heaterSteps.get())
                       .configureTiming(gateTime.get(), heaterTime.get(), intTime.get());

            // Stream results directly to file
            results = measurement.newResults(outputFile.get());

            configurePlots(results);

            // Do the actual measurement now that everything's ready
            measurement.performMeasurement();

            // Check whether it finished because "stop" was pressed or it completing fully
            if (measurement.wasStopped()) {
                GUI.warningAlert("Stopped", "Measurement Stopped", "The measurement was stopped before completion.");
            } else {
                GUI.infoAlert("Complete", "Measurement Completed", "The measurement completed without error.");
            }

        } catch (Exception e) {

            // If something went wrong, output to terminal and show error alert.
            e.printStackTrace();
            GUI.errorAlert("Error", "Exception Encountered", e.getMessage());

        } finally {

            // If we actually got some results, then finalise the table (closes the file)
            if (results != null) {
                results.finalise();
            }

            // Re-enable all the text boxes
            disableInputs(false);
        }

    }

    /**
     * Configures the plots on the tab to display the live results of a new measurement
     *
     * @param results The results object of the new measurement
     */
    private void configurePlots(ResultTable results) {

        // == HEATER POWER PLOT ========================================================================================
        heaterPlot.clear();

        heaterPlot.createSeries()
                  .watch(results, GatedTEM.COL_NUMBER, GatedTEM.COL_HEATER_POWER)
                  .setName("Heater")
                  .setColour(Colour.TEAL);

        // == GATE VOLTAGE PLOT ========================================================================================
        gatePlot.clear();

        // For gate-config 0 (hot-gate)
        gatePlot.createSeries()
                .watch(results, GatedTEM.COL_NUMBER, GatedTEM.COL_GATE_VOLTAGE)
                .filter(r -> r.get(GatedTEM.COL_GATE_CONFIG) == 0.0)
                .setName("Hot-Gate")
                .setColour(Colour.ORANGERED);

        // For gate-config 1 (cold-gate)
        gatePlot.createSeries()
                .watch(results, GatedTEM.COL_NUMBER, GatedTEM.COL_GATE_VOLTAGE)
                .filter(r -> r.get(GatedTEM.COL_GATE_CONFIG) == 1.0)
                .setName("Cold-Gate")
                .setColour(Colour.CORNFLOWERBLUE);

        // == THERMO-VOLTAGE PLOT ======================================================================================
        thermalPlot.clear();

        thermalPlot.createSeries()
                   .watch(results, GatedTEM.COL_NUMBER, GatedTEM.COL_THERMO_VOLTAGE)
                   .setName("Thermo-Voltage")
                   .setColour(Colour.PURPLE);

        // == THERMO VS HEATER PLOT ====================================================================================
        tpPlot.clear();

        // For gate-config 0 (hot-gate)
        tpPlot.createSeries()
              .watch(results, GatedTEM.COL_HEATER_POWER, GatedTEM.COL_THERMO_VOLTAGE)  // Plot HP on x, TV on y
              .filter(r -> r.get(GatedTEM.COL_GATE_CONFIG) == 0.0)                     // Only want gate config 0
              .split(GatedTEM.COL_GATE_SET_VOLTAGE);                                   // Split by set gate voltage

        // For gate-config 1 (cold-gate)
        tpPlot.createSeries()
              .watch(results, GatedTEM.COL_HEATER_POWER, GatedTEM.COL_THERMO_VOLTAGE)  // Plot HP on x, TV on y
              .filter(r -> r.get(GatedTEM.COL_GATE_CONFIG) == 1.0)                     // Only want gate config 1
              .split(GatedTEM.COL_GATE_SET_VOLTAGE);                                   // Split by set gate voltage

        table.clear();
        table.watchList(results);

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

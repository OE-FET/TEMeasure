package TEMeasure.Measurement;

import JISA.Devices.DeviceException;
import JISA.Devices.SMU;
import JISA.Devices.TController;
import JISA.Experiment.ResultList;
import JISA.Experiment.ResultStream;
import JISA.Experiment.ResultTable;
import JISA.Util;

import java.io.IOException;

public class GatedTEM {

    private boolean running = false;

    private SMU         thermoVoltage;
    private SMU         hotGate;
    private SMU         coldGate;
    private SMU         heater;
    private TController stage;

    private double gateStart = -40;
    private double gateStop  = 0;
    private int    gateSteps = 9;

    private double heaterStart = 0;
    private double heaterStop  = 5;
    private int    heaterSteps = 6;

    private int    gateDelay   = 20000;       // 20 seconds
    private int    heaterDelay = 10000;       // 10 seconds
    private double intTime     = 10.0 / 50.0; // 10 power-line cycles

    private int currentStep = 0;
    private int numSteps    = gateSteps * heaterSteps;

    private             ResultTable results;
    public static final String[]    COLUMNS = {"No.", "Stage Temperature", "Gate Voltage", "Gate Current", "Heater Voltage", "Heater Current", "Heater Power", "Thermo-Voltage", "Gate Set", "Gate Config"};
    public static final String[]    UNITS   = {"~", "K", "V", "A", "V", "A", "uW", "uV", "V", "~"};

    public static final int COL_NUMBER            = 0;
    public static final int COL_STAGE_TEMPERATURE = 1;
    public static final int COL_GATE_VOLTAGE      = 2;
    public static final int COL_GATE_CURRENT      = 3;
    public static final int COL_HEATER_VOLTAGE    = 4;
    public static final int COL_HEATER_CURRENT    = 5;
    public static final int COL_HEATER_POWER      = 6;
    public static final int COL_THERMO_VOLTAGE    = 7;
    public static final int COL_GATE_SET_VOLTAGE  = 8;
    public static final int COL_GATE_CONFIG       = 9;

    public GatedTEM(SMU thermoVoltageSMU, SMU hotGateSMU, SMU coldGateSMU, SMU heaterSMU, TController stageController) {
        thermoVoltage = thermoVoltageSMU;
        hotGate = hotGateSMU;
        coldGate = coldGateSMU;
        heater = heaterSMU;
        stage = stageController;
    }

    /**
     * Creates a new results table for measurements, stored in memory.
     *
     * @return New ResultTable
     */
    public ResultTable newResults() {
        results = new ResultList(COLUMNS);
        results.setUnits(UNITS);
        return results;
    }

    /**
     * Creates a new results table for measurements that streams directly to a file.
     *
     * @param path File path to stream to
     *
     * @return New ResultTable
     *
     * @throws IOException Upon error opening specified file for writing
     */
    public ResultTable newResults(String path) throws IOException {
        results = new ResultStream(path, COLUMNS);
        results.setUnits(UNITS);
        return results;
    }

    /**
     * Results the current results table being used by the measurement.
     *
     * @return Current ResultTable
     */
    public ResultTable getResults() {
        return results;
    }

    /**
     * Configures the steps to take in gate voltage.
     *
     * @param start Start voltage, in Volts
     * @param stop  End voltage, in Volts
     * @param steps Number of steps
     *
     * @return Self-reference, for chaining
     */
    public GatedTEM configureGate(double start, double stop, int steps) {
        gateStart = start;
        gateStop = stop;
        gateSteps = steps;
        return this;
    }

    /**
     * Configures the steps to take in heater voltage.
     *
     * @param start Start voltage, in Volts
     * @param stop  End voltage, in Volts
     * @param steps Number of steps
     *
     * @return Self-reference, for chaining
     */
    public GatedTEM configureHeater(double start, double stop, int steps) {
        heaterStart = start;
        heaterStop = stop;
        heaterSteps = steps;
        return this;
    }

    /**
     * Configures the timing parameters of the measurement.
     *
     * @param gateHold        Time to hold for after changing the gate voltage, in seconds (1 ms resolution)
     * @param heaterHold      Time to hold for after changing heater voltage, in seconds (1 ms resolution)
     * @param integrationTime Integration time for the thermo-voltage measurement, in seconds
     *
     * @return Self-reference, for chaining
     */
    public GatedTEM configureTiming(double gateHold, double heaterHold, double integrationTime) {
        gateDelay = (int) (gateHold * 1000); // Convert to milliseconds
        heaterDelay = (int) (heaterHold * 1000); // Convert to milliseconds
        intTime = integrationTime;
        return this;
    }

    /**
     * Perform the measurement, using the configured parameters.
     *
     * @throws IOException     Upon communications error with an instrument
     * @throws DeviceException Upon incompatibility error with an instrument
     */
    public void performMeasurement() throws IOException, DeviceException {

        try {

            running = true;

            currentStep = 0;
            numSteps = gateSteps * heaterSteps;

            // Make sure outputs are disabled to begin with
            thermoVoltage.turnOff();
            hotGate.turnOff();
            coldGate.turnOff();
            heater.turnOff();

            thermoVoltage.useFourProbe(false);
            hotGate.useFourProbe(false);
            coldGate.useFourProbe(false);
            heater.useFourProbe(false);

            // Set integration time of thermo-voltage smu
            thermoVoltage.setIntegrationTime(intTime);
            thermoVoltage.useAutoRanges();
            thermoVoltage.setCurrent(0.0);

            hotGate.useAutoRanges();
            hotGate.setVoltage(gateStart);
            coldGate.useAutoRanges();
            coldGate.setVoltage(gateStart);

            heater.useAutoRanges();
            heater.setVoltage(heaterStart);

            double[] gates   = Util.makeLinearArray(gateStart, gateStop, gateSteps);
            double[] heaters = Util.makeLinearArray(heaterStart, heaterStop, heaterSteps);

            double config = 0;
            mainLoop:
            for (SMU gate : new SMU[]{hotGate, coldGate}) {

                thermoVoltage.turnOn();
                gate.turnOn();

                if (!running) {
                    break mainLoop;
                }

                for (double G : gates) {

                    gate.setVoltage(G);
                    Util.sleep(gateDelay);

                    heater.setVoltage(heaterStart);
                    heater.turnOn();

                    if (!running) {
                        break mainLoop;
                    }

                    for (double H : heaters) {

                        heater.setVoltage(H);
                        Util.sleep(heaterDelay);

                        double heaterVoltage = heater.getVoltage();
                        double heaterCurrent = heater.getCurrent();
                        double heaterPower = heaterVoltage * heaterCurrent;

                        results.addData(
                                (double) currentStep,
                                stage.getTemperature(),
                                gate.getVoltage(),
                                gate.getCurrent(),
                                heaterVoltage,
                                heaterCurrent,
                                heaterPower / 1e-6,
                                thermoVoltage.getVoltage() / 1e-6,
                                G,
                                config
                        );

                        currentStep++;

                        if (!running) {
                            break mainLoop;
                        }

                    }

                    heater.turnOff();
                    Util.sleep(heaterDelay);

                }

                // Reverse gate voltages so that we go down in gate now
                for(int i=0; i < gates.length/2; i++){
                    double temp = gates[i];
                    gates[i] = gates[gates.length -i -1];
                    gates[gates.length -i -1] = temp;
                }

                // Next iteration will be cold-gate config
                config = 1;
            }

        } finally {

            running = false;

            try {
                heater.turnOff();
                hotGate.turnOff();
                coldGate.turnOff();
                thermoVoltage.turnOff();
            } catch (Exception ignored) {
            }

        }

    }

    public double getPercentageComplete() {
        return 100D * ((double) currentStep) / ((double) numSteps);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }

}

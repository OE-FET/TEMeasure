package TEMeasure.Measurement;

import JISA.Devices.SMU;
import JISA.Devices.TC;
import JISA.Experiment.Measurement;
import JISA.Experiment.ResultTable;
import JISA.Util;

public class GatedTEM extends Measurement {

    // Names and units for columns in our results
    public static final String[] COLUMNS                = {"No.", "Sample Temperature", "Gate Voltage", "Gate Current", "Heater Voltage", "Heater Current", "Heater Power", "Thermo-Voltage", "Gate Set", "Gate Config", "Thermo-Current"};
    public static final String[] UNITS                  = {"~", "K", "V", "A", "V", "A", "W", "V", "V", "~", "A"};
    // Constants to define what each column in our results is meant to be
    public static final int      COL_NUMBER             = 0;  // Measurement Number
    public static final int      COL_SAMPLE_TEMPERATURE = 1;  // Sample Temperature
    public static final int      COL_GATE_VOLTAGE       = 2;  // Gate Voltage
    public static final int      COL_GATE_CURRENT       = 3;  // Gate Leakage Current
    public static final int      COL_HEATER_VOLTAGE     = 4;  // Heater Voltage
    public static final int      COL_HEATER_CURRENT     = 5;  // Heater Current
    public static final int      COL_HEATER_POWER       = 6;  // Heater Power
    public static final int      COL_THERMO_VOLTAGE     = 7;  // Thermo-Voltage
    public static final int      COL_GATE_SET_VOLTAGE   = 8;  // Gate Voltage Set-Point
    public static final int      COL_GATE_CONFIG        = 9;  // Gate Configuration (0=hot-gate, 1=cold-gate)
    public static final int      COL_THERMO_CURRENT     = 10; // Current between hot and cold contacts
    private             SMU      thermoVoltage;
    private             SMU      hotGate;
    private             SMU      coldGate;
    private             SMU      heater;
    private             TC       stage;
    // Parameters, with default values
    private             double   gateStart              = -40;         // -40 Volts
    private             double   gateStop               = 0;           //   0 Volts
    private             int      gateSteps              = 9;           //   9 Steps
    private             double   heaterStart            = 0;           //   0 Volts
    private             double   heaterStop             = 5;           //   5 Volts
    private             int      heaterSteps            = 6;           //   6 Steps
    private             int      gateDelay              = 20000;       //  20 seconds
    private             int      heaterDelay            = 10000;       //  10 seconds
    private             double   intTime                = 10.0 / 50.0; //  10 power-line cycles

    public GatedTEM(SMU thermoVoltageSMU, SMU hotGateSMU, SMU coldGateSMU, SMU heaterSMU, TC stageController) {
        thermoVoltage = thermoVoltageSMU;
        hotGate = hotGateSMU;
        coldGate = coldGateSMU;
        heater = heaterSMU;
        stage = stageController;
    }

    private void configureInstruments() throws Exception {

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
        thermoVoltage.setCurrentRange(10E-12);
        thermoVoltage.setCurrent(0.0);
        thermoVoltage.setVoltageLimit(200.0);

        // Configure gate SMUs
        hotGate.useAutoRanges();
        hotGate.setVoltage(gateStart);
        hotGate.setOffMode(SMU.OffMode.HIGH_IMPEDANCE);
        coldGate.useAutoRanges();
        coldGate.setVoltage(gateStart);
        coldGate.setOffMode(SMU.OffMode.HIGH_IMPEDANCE);

        // Configure heater SMU
        heater.useAutoRanges();
        heater.setVoltage(heaterStart);

    }

    @Override
    public void run() throws Exception {

        ResultTable results = getResults();

        configureInstruments();

        // Create arrays of voltage values to use for gate and heater voltages
        double[] gates   = Util.makeLinearArray(gateStart, gateStop, gateSteps);
        double[] heaters = Util.makeLinearArray(heaterStart, heaterStop, heaterSteps);

        // This number indicates whether we're using hot-gate (0) or cold-gate (1) in our data
        double config = 0;

        int currentStep = 0;

        // Loop over each configuration
        for (SMU gate : new SMU[]{hotGate}) {

            // Turn on this gate and the thermo-voltage SMU
            thermoVoltage.turnOn();
            gate.turnOn();

            // Loop over each gate value we want to use
            for (double G : gates) {

                // Set the gate voltage and wait our gate hold time
                gate.setVoltage(G);
                sleep(gateDelay);

                // Initial values
                heater.setVoltage(heaterStart);
                heater.turnOn();

                for (double H : heaters) {

                    // Set the heater and wait our heater hold time
                    heater.setVoltage(H);
                    sleep(heaterDelay);

                    // Get the heater current and voltage to calculate power
                    double heaterVoltage = heater.getVoltage();
                    double heaterCurrent = heater.getCurrent();
                    double heaterPower   = heaterVoltage * heaterCurrent;

                    // Add data-point to our results
                    results.addData(
                            (double) currentStep,        // Measurement number
                            stage.getTemperature(),      // Sample temperature
                            gate.getVoltage(),           // Gate voltage
                            gate.getCurrent(),           // Gate leakage current
                            heaterVoltage,               // Heater voltage
                            heaterCurrent,               // Heater current
                            heaterPower,                 // Heater power
                            thermoVoltage.getVoltage(),  // Thermo-voltage
                            G,                           // Gate set-point
                            config,                      // Hot-Gate (0) or Cold-Gate (1) ?
                            thermoVoltage.getCurrent()
                    );

                    currentStep++;

                }

                // Turn the heater off and wait our heater hold time
                heater.turnOff();
                sleep(heaterDelay);

            }

            // Reverse gate voltages for next iteration of gate loop
            gates = Util.reverseArray(gates);

            // Next iteration will be in next configuration
            config++;

            // Turn off this gate before using the next
            gate.turnOff();
        }
    }

    @Override
    public void onInterrupt() throws Exception {

    }

    @Override
    public void onFinish() throws Exception {
        heater.turnOff();
        hotGate.turnOff();
        coldGate.turnOff();
        thermoVoltage.turnOff();
    }

    @Override
    public String[] getColumns() {
        return COLUMNS;
    }

    @Override
    public String[] getUnits() {
        return UNITS;
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
        gateDelay = (int) (gateHold * 1000);        // Convert to milliseconds
        heaterDelay = (int) (heaterHold * 1000);    // Convert to milliseconds
        intTime = integrationTime;
        return this;
    }


}

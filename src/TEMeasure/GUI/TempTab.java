package TEMeasure.GUI;

import JISA.Control.Field;
import JISA.Control.RTask;
import JISA.Control.SRunnable;
import JISA.Devices.DeviceException;
import JISA.Devices.TC;
import JISA.Experiment.ResultStream;
import JISA.Experiment.ResultTable;
import JISA.GUI.*;
import javafx.scene.paint.Color;

import java.io.IOException;

public class TempTab extends Grid {

    private RTask       logger;
    private ResultTable log;

    private TC   sample      = null;
    private TC   radiation   = null;
    private TC   firstStage  = null;
    private TC   secondStage = null;
    private Plot tPlot;
    private Plot hPlot;

    public TempTab(MainWindow mainWindow) {

        super("Temperature Control");
        setNumColumns(1);

        tPlot = new Plot("Temperatures", "Time [min]", "Temperature [K]");
        hPlot = new Plot("Heaters", "Time [min]", "Heater Power [%]");
        tPlot.showToolbar(true);
        tPlot.setAutoMode();
        hPlot.showToolbar(true);
        hPlot.setAutoMode();

        Fields         control  = new Fields("Control");
        Field<Double>  setPoint = control.addDoubleField("Set-Point [K]");
        Field<Integer> range    = control.addChoice("Heater Range", "Off (0%)", "Low (1%)", "Medium (10%)", "High (100%)");


        ClickHandler refresh = () -> {

            TC t = mainWindow.tcConfigTab.stage.get();

            if (t != null) {

                try {

                    setPoint.set(t.getTargetTemperature());
                    double ra = t.getHeaterRange();

                    if (ra == 0) {
                        range.set(0);
                    } else {
                        range.set((int) Math.floor(Math.log10(ra)) + 1);
                    }
                } catch (Exception ignored) {

                }

            } else {

                setPoint.set(297.0);
                range.set(0);

            }
        };

        control.addButton("Refresh", refresh);

        control.addButton("Apply", () -> {

            TC tc = mainWindow.tcConfigTab.stage.get();

            if (tc == null) {
                GUI.errorAlert(
                        "Error",
                        "T-Controller Not Configured",
                        "The temperature controller is not properly configured."
                );
                return;
            }

            tc.setTargetTemperature(setPoint.get());

            switch (range.get()) {

                case 0:
                    tc.setHeaterRange(0.0);
                    break;

                case 1:
                    tc.setHeaterRange(1.0);
                    break;

                case 2:
                    tc.setHeaterRange(10.0);
                    break;

                case 3:
                    tc.setHeaterRange(100.0);
                    break;

            }

            tc.useAutoHeater();

        });

        add(control);
        add(tPlot);
        add(hPlot);

        logger = new RTask(2500, () -> log.addData(
                logger.getSecFromStart() / 60.0,
                sample.getTemperature(),
                radiation.getTemperature(),
                firstStage.getTemperature(),
                secondStage.getTemperature(),
                sample.getHeaterPower(),
                radiation.getHeaterPower(),
                firstStage.getHeaterPower(),
                secondStage.getHeaterPower(),
                sample.getPValue(),
                radiation.getPValue(),
                firstStage.getPValue(),
                secondStage.getPValue(),
                sample.getIValue(),
                radiation.getIValue(),
                firstStage.getIValue(),
                secondStage.getIValue(),
                sample.getDValue(),
                radiation.getDValue(),
                firstStage.getDValue(),
                secondStage.getDValue(),
                sample.getTargetTemperature(),
                radiation.getTargetTemperature(),
                firstStage.getTargetTemperature(),
                secondStage.getTargetTemperature()
        ));


        addToolbarButton("Start", () -> {
            connect(
                    mainWindow.tcConfigTab.stage,
                    mainWindow.tcConfigTab.shield,
                    mainWindow.tcConfigTab.fStage,
                    mainWindow.tcConfigTab.sStage
            );
            start();
        });

        addToolbarButton("Stop", () -> logger.stop());

        try {
            refresh.click();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void start() {

        if (logger.isRunning()) {
            return;
        }

        if (sample == null || radiation == null || firstStage == null || secondStage == null) {
            GUI.errorAlert("Error", "Cannot start logger", "Temperature controllers not properly configured");
            return;
        }


        String fileName = String.format("TLog-%d.csv", System.currentTimeMillis());

        try {
            log = new ResultStream(
                    fileName,
                    "Time",
                    "Sample",
                    "Radiation",
                    "First",
                    "Second",
                    "Heater 1",
                    "Heater 2",
                    "Heater 3",
                    "Heater 4",
                    "P1",
                    "P2",
                    "P3",
                    "P4",
                    "I1",
                    "I2",
                    "I3",
                    "I4",
                    "D1",
                    "D2",
                    "D3",
                    "D4",
                    "SP1",
                    "SP2",
                    "SP3",
                    "SP4"
            );
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        tPlot.clear();
        hPlot.clear();

        tPlot.createSeries()
             .watch(log, 0, 1)
             .setName("Sample")
             .setColour(Colour.RED)
             .showMarkers(false);

        tPlot.createSeries()
             .watch(log, 0, 21)
             .setName("Sample SP")
             .setColour(Colour.ORANGE)
             .showMarkers(false);

        tPlot.createSeries()
             .watch(log, 0, 2)
             .setName("Radiation")
             .setColour(Colour.GOLD)
             .showMarkers(false);

        tPlot.createSeries()
             .watch(log, 0, 22)
             .setName("Radiation SP")
             .setColour(Colour.YELLOW)
             .showMarkers(false);

        tPlot.createSeries()
             .watch(log, 0, 3)
             .setName("First Stage")
             .setColour(Colour.GREEN)
             .showMarkers(false);

        tPlot.createSeries()
             .watch(log, 0, 23)
             .setName("First Stage SP")
             .setColour(Colour.LIME)
             .showMarkers(false);

        tPlot.createSeries()
             .watch(log, 0, 4)
             .setName("Second Stage")
             .setColour(Colour.BLUE)
             .showMarkers(false);

        tPlot.createSeries()
             .watch(log, 0, 24)
             .setName("Second Stage SP")
             .setColour(Colour.CORNFLOWERBLUE)
             .showMarkers(false);

        hPlot.createSeries()
             .watch(log, 0, 5)
             .setName("Sample")
             .setColour(Color.RED)
             .showMarkers(false);

        hPlot.createSeries()
             .watch(log, 0, 6)
             .setName("Radiation")
             .setColour(Color.GOLD)
             .showMarkers(false);

        hPlot.createSeries()
             .watch(log, 0, 7)
             .setName("First Stage")
             .setColour(Color.GREEN)
             .showMarkers(false);

        hPlot.createSeries()
             .watch(log, 0, 8)
             .setName("Second Stage")
             .setColour(Color.BLUE)
             .showMarkers(false);

        logger.start();

    }

    private void connect(TCConfig s, TCConfig r, TCConfig fs, TCConfig ss) {
        sample      = s.get();
        radiation   = r.get();
        firstStage  = fs.get();
        secondStage = ss.get();
    }

}

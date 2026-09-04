package com.govmesh.food.govmesh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class SoapSimulationConfig {

    public enum SimulationMode {
        SUCCESS,
        TIMEOUT,
        SERVICE_UNAVAILABLE
    }

    private final AtomicReference<SimulationMode> currentMode = new AtomicReference<>(SimulationMode.SUCCESS);

    public SoapSimulationConfig(@Value("${food.soap.simulation.mode:SUCCESS}") String initialMode) {
        setMode(initialMode);
    }

    public SimulationMode getMode() {
        return currentMode.get();
    }

    public void setMode(String modeStr) {
        if (modeStr == null) {
            currentMode.set(SimulationMode.SUCCESS);
            return;
        }
        try {
            currentMode.set(SimulationMode.valueOf(modeStr.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            currentMode.set(SimulationMode.SUCCESS);
        }
    }
}

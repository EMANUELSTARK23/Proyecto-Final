package com.emanuelgb.sensores.model;

import com.emanuelgb.sensores.exception.IoTException;

public class SensorTemperatura extends SensorBase {
    @Override
    public String getTipo() {
        return "TEMPERATURA";
    }

    @Override
    public double obtenerLectura() throws IoTException {
        if (!activo) {
            throw new IoTException("ConexionPerdidaException: Sensor " + idSensor + " apagado.");
        }
        double val = 18.0 + (Math.random() * 20.0);
        if (val > umbralMax) {
            throw new IoTException("LecturaAnomalaException: Temperatura crítica (" + String.format("%.2f", val) + "°C)");
        }
        return val;
    }
}
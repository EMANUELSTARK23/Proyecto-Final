package com.emanuelgb.sensores.model;

import com.emanuelgb.sensores.exception.IoTException;

public class SensorHumedad extends SensorBase {
    @Override
    public String getTipo() {
        return "HUMEDAD";
    }

    @Override
    public double obtenerLectura() throws IoTException {
        if (!activo) {
            throw new IoTException("ConexionPerdidaException: Sensor de humedad " + idSensor + " sin respuesta.");
        }
        double val = 35.0 + (Math.random() * 55.0);
        if (val > umbralMax) {
            throw new IoTException("LecturaAnomalaException: Humedad excesiva (" + String.format("%.2f", val) + " %)");
        }
        return val;
    }
}
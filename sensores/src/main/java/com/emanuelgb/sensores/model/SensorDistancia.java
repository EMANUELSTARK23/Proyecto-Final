package com.emanuelgb.sensores.model;

import com.emanuelgb.sensores.exception.IoTException;

public class SensorDistancia extends SensorBase {
    @Override
    public String getTipo() {
        return "DISTANCIA";
    }

    @Override
    public double obtenerLectura() throws IoTException {
        if (!activo) {
            throw new IoTException("ConexionPerdidaException: Sensor ultrasónico " + idSensor + " desconectado.");
        }
        double val = 10.0 + (Math.random() * 180.0);
        if (val > umbralMax) {
            throw new IoTException("LecturaAnomalaException: Distancia fuera de límite (" + String.format("%.2f", val) + " cm)");
        }
        return val;
    }
}
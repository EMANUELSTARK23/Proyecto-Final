package com.emanuelgb.sensores.model;

import com.emanuelgb.sensores.exception.IoTException;

public class SensorColor extends SensorBase {
    @Override
    public String getTipo() {
        return "COLOR";
    }

    @Override
    public double obtenerLectura() throws IoTException {
        if (!activo) {
            throw new IoTException("ConexionPerdidaException: Sensor " + idSensor + " desconectado.");
        }
        double val = 200.0 + (Math.random() * 400.0);
        if (val > umbralMax) {
            throw new IoTException("LecturaAnomalaException: Frecuencia anómala (" + String.format("%.2f", val) + "Hz)");
        }
        return val;
    }
}
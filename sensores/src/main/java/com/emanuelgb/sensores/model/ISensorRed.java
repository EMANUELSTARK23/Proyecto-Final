package com.emanuelgb.sensores.model;

import com.emanuelgb.sensores.exception.IoTException;

public interface ISensorRed {
    double obtenerLectura() throws IoTException;
    String getTipo();
}
package com.emanuelgb.sensores.model;

public abstract class SensorBase implements ISensorRed {
    protected String idSensor;
    protected String nombre;
    protected int puerto;
    protected double umbralMax;
    protected boolean activo;

    public String getIdSensor() { return idSensor; }
    public void setIdSensor(String idSensor) { this.idSensor = idSensor; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getPuerto() { return puerto; }
    public void setPuerto(int puerto) { this.puerto = puerto; }

    public double getUmbralMax() { return umbralMax; }
    public void setUmbralMax(double umbralMax) { this.umbralMax = umbralMax; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
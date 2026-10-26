package com.panaderialocal.conexion.modelos;

import java.time.LocalDate;

public class Produccion {

    private int idProduccion;
    private LocalDate fecha;
    private int idPan;
    private String nombrePan;
    private int cantidadProducida;
    private int cantidadVendida;
    private int cantidadSobrante;
    private int merma;

    public Produccion() {
    }

    public Produccion(int idProduccion, LocalDate fecha, int idPan,
            String nombrePan, int cantidadProducida,
            int cantidadVendida, int cantidadSobrante, int merma) {
        this.idProduccion = idProduccion;
        this.fecha = fecha;
        this.idPan = idPan;
        this.nombrePan = nombrePan;
        this.cantidadProducida = cantidadProducida;
        this.cantidadVendida = cantidadVendida;
        this.cantidadSobrante = cantidadSobrante;
        this.merma = merma;
    }

    public int getIdProduccion() {
        return idProduccion;
    }

    public void setIdProduccion(int idProduccion) {
        this.idProduccion = idProduccion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getIdPan() {
        return idPan;
    }

    public void setIdPan(int idPan) {
        this.idPan = idPan;
    }

    public String getNombrePan() {
        return nombrePan;
    }

    public void setNombrePan(String nombrePan) {
        this.nombrePan = nombrePan;
    }

    public int getCantidadProducida() {
        return cantidadProducida;
    }

    public void setCantidadProducida(int cantidadProducida) {
        this.cantidadProducida = cantidadProducida;
    }

    public int getCantidadVendida() {
        return cantidadVendida;
    }

    public void setCantidadVendida(int cantidadVendida) {
        this.cantidadVendida = cantidadVendida;
    }

    public int getCantidadSobrante() {
        return cantidadSobrante;
    }

    public void setCantidadSobrante(int cantidadSobrante) {
        this.cantidadSobrante = cantidadSobrante;
    }

    public int getMerma() {
        return merma;
    }

    public void setMerma(int merma) {
        this.merma = merma;
    }
}
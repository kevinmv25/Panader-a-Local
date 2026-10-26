
package com.mycompany.panaderia.local;



public class Pan {
    private int idPan;
    private String nombre;
    private String descripcion;
    private double precio;
    private String foto;

    public Pan(int idPan, String nombre, String descripcion, double precio, String foto) {
        this.idPan = idPan;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.foto = foto;
    }

    public int getIdPan() { return idPan; }
    public String getNombre() { return nombre; }
    public String getdescripcion() { return descripcion; }
    public double getPrecio() { return precio; }
    public String getFoto() { return foto; }
}
package com.example.provaaf;

public class Place {
    public String id;
    public String nome;
    public String tipo;
    public String categoria;
    public String endereco;
    public double lat;
    public double lon;
    public String observacao;
    public transient double distancia; // metros // não ta salvo no Firebase
    public Place() {}

    public Place(String nome, String tipo, String endereco, double lat, double lon) {
        this.nome     = nome;
        this.tipo     = tipo;
        this.endereco = endereco;
        this.lat      = lat;
        this.lon      = lon;
    }

    @Override
    public String toString() {
        String loc = (endereco != null && !endereco.isEmpty()) ? endereco : String.format(java.util.Locale.US, "%.5f, %.5f", lat, lon);
        String texto = (nome != null ? nome : "Sem nome") + "\nTipo: " + tipo + "\n" + loc;
        if (categoria != null && !categoria.isEmpty()) texto += " | " + categoria;
        if (observacao != null && !observacao.isEmpty()) texto += "\nObs: " + observacao;
        return texto;
    }
}
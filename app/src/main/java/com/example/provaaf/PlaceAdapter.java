package com.example.provaaf;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;


public class PlaceAdapter extends ArrayAdapter<Place> {

    public PlaceAdapter(Context context, List<Place> lista) {
        super(context, R.layout.item_place, lista);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place, parent, false);
        }

        Place p = getItem(position);
        if (p == null) return convertView;

        TextView tvNome = convertView.findViewById(R.id.tvNome);
        TextView tvTipo = convertView.findViewById(R.id.tvTipo);
        TextView tvEndereco = convertView.findViewById(R.id.tvEndereco);
        TextView tvCoordenadas = convertView.findViewById(R.id.tvCoordenadas);
        TextView tvDistancia = convertView.findViewById(R.id.tvDistancia);
        TextView tvCategoria = convertView.findViewById(R.id.tvCategoria);
        TextView tvObs = convertView.findViewById(R.id.tvObs);

        tvNome.setText(p.nome != null ? p.nome : "Sem nome");
        tvTipo.setText(p.tipo != null ? p.tipo : "");
        if (p.endereco != null && !p.endereco.isEmpty()) {
            tvEndereco.setText(p.endereco);
            tvEndereco.setVisibility(View.VISIBLE);
        } else {
            tvEndereco.setVisibility(View.GONE);
        }

        if (p.lat != 0 || p.lon != 0) {
            tvCoordenadas.setText(String.format(java.util.Locale.US, "%.5f, %.5f", p.lat, p.lon));
            tvCoordenadas.setVisibility(View.VISIBLE);
        } else {
            tvCoordenadas.setVisibility(View.GONE);
        }

        if (p.distancia > 0) {
            tvDistancia.setText(formatarDistancia(p.distancia));
            tvDistancia.setVisibility(View.VISIBLE);
        } else {
            tvDistancia.setVisibility(View.GONE);
        }

        if (p.categoria != null && !p.categoria.isEmpty()) {
            tvCategoria.setText("● " + p.categoria);
            tvCategoria.setVisibility(View.VISIBLE);
        } else {
            tvCategoria.setVisibility(View.GONE);
        }

        if (p.observacao != null && !p.observacao.isEmpty()) {
            tvObs.setText("\"" + p.observacao + "\"");
            tvObs.setVisibility(View.VISIBLE);
        } else {
            tvObs.setVisibility(View.GONE);
        }

        return convertView;
    }

    private String formatarDistancia(double metros) {
        if (metros < 1000)
            return String.format(java.util.Locale.US, "~%.0f m", metros);
        return String.format(java.util.Locale.US, "~%.1f km", metros / 1000);
    }
}

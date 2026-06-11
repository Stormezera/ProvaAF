package com.example.provaaf;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.*;
import com.google.firebase.database.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    static final String[] CATEGORIAS  = {"Saúde", "Estudo", "Lazer", "Alimentação", "Compras", "Outros"};
    static final String[] TIPOS_BUSCA = {"Farmácia", "Hospital", "Escola", "Restaurante", "Praça", "Mercado"};
    TextView tvLocalizacao;
    Spinner spBusca;
    ListView listView;
    Button btnBuscar, btnAlternar;
    double lat = 0, lon = 0;
    List<Place> resultados = new ArrayList<>();
    List<Place> salvos = new ArrayList<>();
    boolean mostraSalvos = false;
    FusedLocationProviderClient fusedClient;
    DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseDatabase.getInstance().getReference("places");

        tvLocalizacao = findViewById(R.id.tvLocalizacao);
        spBusca       = findViewById(R.id.spBusca);
        spBusca.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TIPOS_BUSCA));
        listView      = findViewById(R.id.listView);
        btnBuscar     = findViewById(R.id.btnBuscar);
        btnAlternar   = findViewById(R.id.btnAlternar);

        // Firebase
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                salvos.clear();
                for (DataSnapshot filho : snapshot.getChildren()) {
                    Place p = filho.getValue(Place.class);
                    if (p != null) {
                        p.id = filho.getKey();
                        salvos.add(p);
                    }
                }
                atualizarLista();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erro Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnBuscar.setOnClickListener(v -> buscarLocais());

        btnAlternar.setOnClickListener(v -> {
            mostraSalvos = !mostraSalvos;
            btnAlternar.setText(mostraSalvos ? "Ver Resultados" : "Ver Salvos");
            atualizarLista();
        });

        // salvar (resultado) ou editar (salvo)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Place p = (Place) parent.getItemAtPosition(position);
            if (mostraSalvos) abrirDialogEditar(p);
            else abrirDialogSalvar(p);
        });

        // longo: excluir salvo
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (mostraSalvos) {
                Place p = (Place) parent.getItemAtPosition(position);
                new AlertDialog.Builder(this).setMessage("Excluir \"" + p.nome + "\"?")
                        .setPositiveButton("Sim", (d, w) -> db.child(p.id).removeValue())
                        .setNegativeButton("Não", null).show();
            }
            return true;
        });

        pedirLocalizacao();
    }

    void pedirLocalizacao() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            obterLocalizacao();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacao();
        } else {
            tvLocalizacao.setText("Permissão de localização negada.");
        }
    }

    @SuppressWarnings("MissingPermission")
    void obterLocalizacao() {
        tvLocalizacao.setText("Obtendo localização...");
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        lat = loc.getLatitude();
                        lon = loc.getLongitude();
                        exibirNomeRua(lat, lon);
                    } else {
                        tvLocalizacao.setText("Não foi possível obter localização.");
                    }
                });
    }

    // exibir o nome da rua
    void exibirNomeRua(double lat, double lon) {
        new Thread(() -> {
            String resultado = String.format(Locale.US, "%.5f, %.5f", lat, lon);
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("pt", "BR"));
                List<Address> lista = geocoder.getFromLocation(lat, lon, 1);
                if (lista != null && !lista.isEmpty()) {
                    Address a = lista.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (a.getThoroughfare() != null)  sb.append(a.getThoroughfare());
                    if (a.getSubThoroughfare() != null) sb.insert(0, a.getSubThoroughfare() + " ");
                    if (a.getSubLocality() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(a.getSubLocality());
                    }
                    if (sb.length() > 0) resultado = sb.toString();
                }
            } catch (IOException ignored) {}
            String endFinal = resultado;
            runOnUiThread(() -> tvLocalizacao.setText("📍 " + endFinal));
        }).start();
    }

    // ---- busca -- Nominatim ----
    void buscarLocais() {
        String texto = TIPOS_BUSCA[spBusca.getSelectedItemPosition()];
        if (lat == 0 && lon == 0) {
            Toast.makeText(this, "Aguardando localização...", Toast.LENGTH_SHORT).show();
            return;
        }

        String viewbox = String.format(Locale.US, "%.4f,%.4f,%.4f,%.4f", lon - 0.05, lat + 0.05, lon + 0.05, lat - 0.05);
        Toast.makeText(this, "Buscando " + texto + "...", Toast.LENGTH_SHORT).show();
        btnBuscar.setEnabled(false);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("User-Agent", "ProvaAF/1.0").build())).build();

        NominatimService api = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/").client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(NominatimService.class);

        api.buscar(texto, "json", 20, viewbox, 0, "pt").enqueue(new Callback<List<NominatimResult>>() {
            @Override
            public void onResponse(Call<List<NominatimResult>> call, Response<List<NominatimResult>> response) {
                btnBuscar.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(MainActivity.this, "Erro: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                resultados.clear();
                for (NominatimResult r : response.body()) {
                    double pLat = 0, pLon = 0;
                    try { pLat = Double.parseDouble(r.lat); } catch (Exception ignored) {}
                    try { pLon = Double.parseDouble(r.lon); } catch (Exception ignored) {}

                    String nome = r.shortName();
                    String tipo = (r.type != null) ? r.type : texto;
                    String endereco = r.streetAddress();

                    Place p = new Place(nome, tipo, endereco, pLat, pLon);
                    p.distancia = calcularDistancia(lat, lon, pLat, pLon);
                    resultados.add(p);
                }
                mostraSalvos = false;
                btnAlternar.setText("Ver Salvos");
                atualizarLista();
                if (resultados.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Nenhum resultado.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<NominatimResult>> call, Throwable t) {
                btnBuscar.setEnabled(true);
                Toast.makeText(MainActivity.this, "Falha de rede: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // distância em metros entre dois pontos geográficos
    double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6_371_000;
        double phi1    = Math.toRadians(lat1);
        double phi2    = Math.toRadians(lat2);
        double dPhi    = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2) + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    void abrirDialogSalvar(Place p) {
        EditText etObs = new EditText(this);
        etObs.setHint("Observação (opcional)");
        Spinner spCategoria = new Spinner(this);
        spCategoria.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIAS));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(etObs);
        layout.addView(spCategoria);

        new AlertDialog.Builder(this)
        .setTitle("Salvar: " + p.nome).setView(layout)
        .setPositiveButton("Salvar", (d, w) -> {
            p.observacao = etObs.getText().toString().trim();
            p.categoria  = CATEGORIAS[spCategoria.getSelectedItemPosition()];
            db.push().setValue(p, (erro, ref) -> {
                if (erro != null) {
                    Toast.makeText(this, "Erro ao salvar: " + erro.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
                }
            });
        }).setNegativeButton("Cancelar", null).show();
    }

    void abrirDialogEditar(Place p) {
        EditText etObs = new EditText(this);
        etObs.setText(p.observacao);

        Spinner spCategoria = new Spinner(this);
        spCategoria.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CATEGORIAS));

        for (int i = 0; i < CATEGORIAS.length; i++) {
            if (CATEGORIAS[i].equals(p.categoria)) { spCategoria.setSelection(i); break; }
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(etObs);
        layout.addView(spCategoria);

    new AlertDialog.Builder(this)
        .setTitle("Editar: " + p.nome)
        .setView(layout)
        .setPositiveButton("Salvar", (d, w) -> {
            p.observacao = etObs.getText().toString().trim();
            p.categoria  = CATEGORIAS[spCategoria.getSelectedItemPosition()];
            db.child(p.id).setValue(p, (erro, ref) -> {
                if (erro != null) {
                    Toast.makeText(this, "Erro ao atualizar: " + erro.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                }
            });
        }).setNegativeButton("Cancelar", null).show();
    }

    void atualizarLista() {
        List<Place> dados = mostraSalvos ? salvos : resultados;
        listView.setAdapter(new PlaceAdapter(this, dados));
    }
}
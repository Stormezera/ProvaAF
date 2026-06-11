package com.example.provaaf;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Documentação: https://nominatim.org/release-docs/latest/api/Search/
public interface NominatimService {

    @GET("search")
    Call<List<NominatimResult>> buscar(
            @Query("q")               String query,
            @Query("format")          String format,
            @Query("limit")           int limit,
            @Query("viewbox")         String viewbox,
            @Query("bounded")         int bounded,
            @Query("accept-language") String lang
    );
}

// Resultado da Nominatim API
class NominatimResult {
    @SerializedName("display_name")
    public String displayName;
    public String lat;
    public String lon;
    public String type;
    @SerializedName("class")
    public String placeClass;

    public String shortName() {
        if (displayName == null) return "Sem nome";
        int virgula = displayName.indexOf(',');
        return virgula > 0 ? displayName.substring(0, virgula).trim() : displayName.trim();
    }

    public String streetAddress() {
        if (displayName == null) return "";
        String[] partes = displayName.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < Math.min(partes.length, 3); i++) {
            if (sb.length() > 0) sb.append(",");
            sb.append(partes[i].trim());
        }
        return sb.toString();
    }
}

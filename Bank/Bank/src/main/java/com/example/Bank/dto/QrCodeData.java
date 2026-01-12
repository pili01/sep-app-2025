package com.example.Bank.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
public class QrCodeData {
    private String K = "PR";    //Tip QR koda	PR → plaćanje računa / usluge
    private String V = "01";    //Verzija IPS standarda	01
    private String C = "1";     //Kodiranje	1(UTF-8)
    private String R;           //Račun primaoca, tačno 18cifara
    private String N;           //Naziv primaoca
    private String I;           //Iznos RSD
    private String SF = "189";  //Šifra plaćanja 189
    private String S;           //Svrha plaćanja
    private String RO;          //Poziv na broj odobrenja + id transakcije

    @AssertTrue
    public boolean isValid() {
        return R != null && R.matches("\\d{18}") &&
                N != null && !N.isEmpty() &&
                I != null && I.matches("\\d+(\\.\\d{1,2})?") &&
                S != null && !S.isEmpty() &&
                RO != null && !RO.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("K:").append(K).append("|");
        sb.append("V:").append(V).append("|");
        sb.append("C:").append(C).append("|");
        sb.append("R:").append(R).append("|");
        sb.append("N:").append(N).append("|");
        sb.append("I:").append(I).append("|");
        sb.append("SF:").append(SF).append("|");
        sb.append("S:").append(S).append("|");
        sb.append("RO:").append(RO);
        return sb.toString();
    }

    private String formatAmountRsd(double amount) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("sr-RS"));
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');

        DecimalFormat df = new DecimalFormat("0.##", sym); // zarez je decimalni, nule nisu obavezne :contentReference[oaicite:17]{index=17}
        return "RSD" + df.format(amount);
    }

    public void setI(Double amount) {
        this.I = formatAmountRsd(amount);
    }
}

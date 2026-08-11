package com.campanha.eleitores.application.port.out;

import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.domain.RegiaoEleitoral;

import java.util.List;
import java.util.Optional;

public interface RegiaoGeoPort {
    /** Retorna a região mais fina (BAIRRO_ZONA se existir) que contém o ponto. */
    Optional<RegiaoEleitoral> encontrarRegiaoMaisFinaContendo(Ponto ponto);

    List<RegiaoEleitoral> listarTodas();
}

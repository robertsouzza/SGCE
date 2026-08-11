package com.campanha.eleitores.application.port.in;

import com.campanha.eleitores.domain.Abordagem;
import com.campanha.eleitores.domain.Eleitor;
import com.campanha.eleitores.domain.Intencao;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.domain.RegiaoEleitoral;
import com.campanha.eleitores.domain.TipoAbordagem;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EleitoresUseCases {

    Eleitor cadastrarEleitor(CadastrarEleitorCommand cmd);

    Abordagem registrarAbordagem(RegistrarAbordagemCommand cmd);

    Optional<RegiaoEleitoral> consultarRegiaoPorGeo(Ponto ponto);

    ResultadoLoteSync sincronizarLote(List<OperacaoSync> operacoes, Long membroId);

    Eleitor anonimizarEleitor(Long eleitorId);

    List<Eleitor> listarEleitores();

    record CadastrarEleitorCommand(
            String nomeCompleto,
            String endereco,
            Ponto geolocalizacao,
            String telefoneWhatsapp,
            String tituloEleitor,
            String zonaEleitoral,
            String secaoEleitoral,
            String observacoes
    ) {}

    record IntencaoInput(Long candidatoId, Intencao intencao) {}

    record RegistrarAbordagemCommand(
            Long eleitorId,
            Long equipeId,
            Long membroId,
            TipoAbordagem tipoAbordagem,
            Instant dataHora,
            Ponto geolocalizacao,
            Instant timestampLocal,
            List<IntencaoInput> intencoes
    ) {}

    /** Uma operação recebida do app offline no batch sync. */
    record OperacaoSync(
            UUID clientOpId,
            String entidade,
            String operacao,
            Object payload,
            Instant timestampLocal
    ) {}

    record ResultadoOpSync(
            UUID clientOpId,
            String status,
            Long serverId,
            String mensagem
    ) {}

    record ResultadoLoteSync(List<ResultadoOpSync> resultados) {}
}

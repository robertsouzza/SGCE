package com.campanha.eleitores.application.service;

import com.campanha.auditoria.domain.Auditavel;
import com.campanha.eleitores.application.port.in.EleitoresUseCases;
import com.campanha.eleitores.application.port.out.AbordagemRepositoryPort;
import com.campanha.eleitores.application.port.out.EleitorRepositoryPort;
import com.campanha.eleitores.application.port.out.RegiaoGeoPort;
import com.campanha.eleitores.domain.Abordagem;
import com.campanha.eleitores.domain.Eleitor;
import com.campanha.eleitores.domain.IntencaoVoto;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.domain.RegiaoEleitoral;
import com.campanha.shared.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EleitoresService implements EleitoresUseCases {

    private final EleitorRepositoryPort eleitorRepo;
    private final AbordagemRepositoryPort abordagemRepo;
    private final RegiaoGeoPort regiaoGeo;
    private final SincronizacaoService sincronizacaoService;

    @Override
    @Transactional
    @Auditavel(acao = "cadastrar_eleitor", entidade = "Eleitor")
    public Eleitor cadastrarEleitor(CadastrarEleitorCommand cmd) {
        Long partido = tenantObrigatorio();
        // Se já existe (upsert por chave natural), atualiza; senão cria.
        Optional<Eleitor> existente = eleitorRepo.findByTituloEleitorAndPartidoId(cmd.tituloEleitor(), partido);
        Eleitor novo = new Eleitor(
                existente.map(Eleitor::id).orElse(null),
                partido,
                cmd.nomeCompleto(),
                cmd.endereco(),
                cmd.geolocalizacao(),
                cmd.telefoneWhatsapp(),
                cmd.tituloEleitor(),
                null,
                cmd.zonaEleitoral(),
                cmd.secaoEleitoral(),
                cmd.observacoes(),
                false,
                null,
                existente.map(Eleitor::criadoEm).orElse(Instant.now()),
                Instant.now()
        );
        return eleitorRepo.save(novo);
    }

    @Override
    @Transactional
    @Auditavel(acao = "registrar_abordagem", entidade = "Abordagem")
    public Abordagem registrarAbordagem(RegistrarAbordagemCommand cmd) {
        Long partido = tenantObrigatorio();
        List<IntencaoVoto> intencoes = new ArrayList<>();
        if (cmd.intencoes() != null) {
            for (IntencaoInput i : cmd.intencoes()) {
                intencoes.add(new IntencaoVoto(null, partido, null, i.candidatoId(), i.intencao()));
            }
        }
        Abordagem a = new Abordagem(
                null, partido, cmd.eleitorId(), cmd.membroId(), cmd.equipeId(),
                cmd.tipoAbordagem(), cmd.dataHora(), cmd.geolocalizacao(),
                cmd.timestampLocal(), Instant.now(), true,
                intencoes, Instant.now()
        );
        return abordagemRepo.save(a);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegiaoEleitoral> consultarRegiaoPorGeo(Ponto ponto) {
        return regiaoGeo.encontrarRegiaoMaisFinaContendo(ponto);
    }

    @Override
    @Transactional
    public ResultadoLoteSync sincronizarLote(List<OperacaoSync> operacoes, Long membroId) {
        Long partido = tenantObrigatorio();
        return sincronizacaoService.processarLote(operacoes, partido, membroId);
    }

    @Override
    @Transactional
    @Auditavel(acao = "anonimizar_eleitor", entidade = "Eleitor")
    public Eleitor anonimizarEleitor(Long eleitorId) {
        Eleitor e = eleitorRepo.findById(eleitorId)
                .orElseThrow(() -> new IllegalArgumentException("eleitor não encontrado"));
        return eleitorRepo.save(e.anonimizar());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Eleitor> listarEleitores() {
        return eleitorRepo.findAll();
    }

    private Long tenantObrigatorio() {
        Long t = TenantContext.get();
        if (t == null) {
            throw new AccessDeniedException("operação requer contexto de partido");
        }
        return t;
    }
}

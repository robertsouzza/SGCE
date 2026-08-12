package com.campanha.eleitores.infrastructure.adapter.in.web;

import com.campanha.eleitores.application.port.in.EleitoresUseCases;
import com.campanha.eleitores.domain.Eleitor;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.domain.RegiaoEleitoral;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eleitores")
@RequiredArgsConstructor
public class EleitorController {

    private final EleitoresUseCases uc;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public ResponseEntity<Eleitor> cadastrar(@Valid @RequestBody CadastrarEleitorRequest req) {
        Ponto geo = req.geolocalizacao() == null ? null
                : new Ponto(req.geolocalizacao().longitude(), req.geolocalizacao().latitude());
        Eleitor e = uc.cadastrarEleitor(new EleitoresUseCases.CadastrarEleitorCommand(
                req.nomeCompleto(), req.endereco(), geo, req.telefoneWhatsapp(),
                req.tituloEleitor(), req.zonaEleitoral(), req.secaoEleitoral(), req.observacoes()));
        return ResponseEntity.created(URI.create("/api/eleitores/" + e.id())).body(e);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE','CANDIDATO')")
    public List<Eleitor> listar() {
        return uc.listarEleitores();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE','CANDIDATO')")
    public ResponseEntity<Eleitor> buscarPorId(@PathVariable Long id) {
        return uc.buscarEleitor(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/regiao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegiaoEleitoral> regiaoPorEleitor(@PathVariable Long id) {
        return uc.buscarEleitor(id)
                .flatMap(e -> uc.consultarRegiaoPorGeo(e.geolocalizacao()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/anonimizar")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE')")
    public Eleitor anonimizar(@PathVariable Long id) {
        return uc.anonimizarEleitor(id);
    }

    /**
     * Contagem agregada sem PII — usado pelo dashboard e para provar que
     * agregados sobrevivem à revogação de consentimento_dados (D-02).
     * Passa pelo use case @Transactional para o TenantAwareTransactionAspect
     * aplicar o SET LOCAL app.current_partido_id (sem isso, RLS filtra tudo).
     */
    @GetMapping("/agregado")
    @PreAuthorize("hasAnyRole('ADMIN','LIDER_EQUIPE','MEMBRO_EQUIPE','CANDIDATO')")
    public Map<String, Object> agregadoPorRegiao(@RequestParam Long regiaoId) {
        return Map.of(
                "regiaoId", regiaoId,
                "totalEleitores", uc.contarEleitoresPorRegiao(regiaoId)
        );
    }

    public record CadastrarEleitorRequest(
            @NotBlank String nomeCompleto,
            String endereco,
            GeoLocalizacaoRequest geolocalizacao,
            String telefoneWhatsapp,
            @NotBlank String tituloEleitor,
            String zonaEleitoral,
            String secaoEleitoral,
            String observacoes
    ) {}

    public record GeoLocalizacaoRequest(double longitude, double latitude) {}
}

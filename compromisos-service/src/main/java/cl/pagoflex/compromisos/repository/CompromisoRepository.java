package cl.pagoflex.compromisos.repository;

import cl.pagoflex.compromisos.model.Compromiso;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

    List<Compromiso> findByIdUsuario(Long idUsuario);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Compromiso c
            SET c.estado = 'PAGADO'
            WHERE c.idCompromiso = :id
              AND c.estado = 'PENDIENTE'
              AND c.activo = true
            """)
    int marcarComoPagadoSiPendiente(@Param("id") Long id);
}

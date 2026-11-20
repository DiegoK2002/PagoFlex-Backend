package cl.pagoflex.archivos.service;

import cl.pagoflex.archivos.client.*;
import cl.pagoflex.archivos.model.Archivo;
import cl.pagoflex.archivos.repository.ArchivoRepository;
import feign.FeignException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


class ArchivoCoberturaTest {
    @TempDir Path temporal;
    ArchivoRepository repo;
    UsuariosClient usuarios;
    PagosClient pagos;
    ArchivoService service;
    @BeforeEach void preparar() {
        repo=mock(ArchivoRepository.class);
        usuarios=mock(UsuariosClient.class);
        pagos=mock(PagosClient.class);
        service=new ArchivoService(repo,pagos,usuarios,temporal.toString());
    }
    MockMultipartFile png() {
        return new MockMultipartFile("archivo","avatar.png","image/png",
            new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,1});
    }
    MockMultipartFile archivo(String nombre,String mime,byte[] bytes) {
        return new MockMultipartFile("archivo",nombre,mime,bytes);
    }
    void usuarioActivo() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
    }
    @Test void idUsuarioNuloEsInvalido() { assertStatus(HttpStatus.BAD_REQUEST,null,null,"AVATAR",png()); }
    @Test void idPagoCeroEsInvalido() { assertStatus(HttpStatus.BAD_REQUEST,7L,0L,"AVATAR",png()); }
    @Test void tipoNullEsInvalido() { assertStatus(HttpStatus.BAD_REQUEST,7L,null,null,png()); }
    @Test void tipoVacioEsInvalido() { assertStatus(HttpStatus.BAD_REQUEST,7L,null,"  ",png()); }
    @Test void tipoNoPermitidoEsInvalido() { assertStatus(HttpStatus.BAD_REQUEST,7L,null,"MUSICA",png()); }
    @Test void archivoNuloEsInvalido() { assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",null); }
    @Test void archivoVacioEsInvalido() {
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("x.png","image/png",new byte[0]));
    }
    @Test void archivoMayorA10MBEsInvalido() {
        MultipartFile grande=mock(MultipartFile.class);
        when(grande.getSize()).thenReturn(10L*1024*1024+1);
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",grande);
    }
    @Test void nombreNuloEsInvalido() {
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo(null,"image/png",new byte[]{1}));
    }
    @Test void nombreMuyLargoEsInvalido() {
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("a".repeat(256),"image/png",new byte[]{1}));
    }
    @Test void nombreConCaracterDeControlEsInvalido() {
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("a\nb.png","image/png",new byte[]{1}));
    }
    @Test void sinExtensionEsInvalido() {
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("archivo","image/png",new byte[]{1}));
    }
    @Test void mimeQueNoCoincideConExtensionEsInvalido() {
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("avatar.png","application/pdf",new byte[]{1}));
    }
    @Test void pdfNoSePermiteComoAvatar() {
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("avatar.pdf","application/pdf",new byte[]{'%','P','D','F','-'}));
    }
    @Test void contenidoPngConFirmaInvalidaEsRechazado() {
        usuarioActivo();
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("avatar.png","image/png",new byte[]{1,2,3,4,5,6,7,8}));
    }
    @Test void contenidoPdfConFirmaInvalidaEsRechazado() {
        usuarioActivo();
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"ADJUNTO",archivo("reporte.pdf","application/pdf",new byte[]{1,2,3,4,5}));
    }
    @Test void contenidoJpgConFirmaInvalidaEsRechazado() {
        usuarioActivo();
        assertStatus(HttpStatus.BAD_REQUEST,7L,null,"AVATAR",archivo("imagen.jpg","image/jpeg",new byte[]{1,2,3}));
    }
    @Test void usuarioNoEncontrado() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.NotFound.class));
        assertStatus(HttpStatus.NOT_FOUND,7L,null,"AVATAR",png());
    }
    @Test void usuarioRemotoCaido() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.class));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE,7L,null,"AVATAR",png());
    }
    @Test void usuarioConIdDiferente() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(8L,true));
        assertStatus(HttpStatus.BAD_GATEWAY,7L,null,"AVATAR",png());
    }
    @Test void usuarioNulo() { assertStatus(HttpStatus.BAD_GATEWAY,7L,null,"AVATAR",png()); }
    @Test void usuarioInactivo() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,false));
        assertStatus(HttpStatus.CONFLICT,7L,null,"AVATAR",png());
    }
    @Test void pagoNoEncontrado() {
        usuarioActivo();
        when(pagos.buscarPorId(23L)).thenThrow(mock(FeignException.NotFound.class));
        assertStatus(HttpStatus.NOT_FOUND,7L,23L,"COMPROBANTE",png());
    }
    @Test void pagoRemotoCaido() {
        usuarioActivo();
        when(pagos.buscarPorId(23L)).thenThrow(mock(FeignException.class));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE,7L,23L,"COMPROBANTE",png());
    }
    @Test void pagoNulo() {
        usuarioActivo();
        assertStatus(HttpStatus.BAD_GATEWAY,7L,23L,"COMPROBANTE",png());
    }
    @Test void pagoConIdDistinto() {
        usuarioActivo();
        when(pagos.buscarPorId(23L)).thenReturn(new PagosClient.PagoResumen(24L,7L));
        assertStatus(HttpStatus.BAD_GATEWAY,7L,23L,"COMPROBANTE",png());
    }
    @Test void pagoDeOtroUsuario() {
        usuarioActivo();
        when(pagos.buscarPorId(23L)).thenReturn(new PagosClient.PagoResumen(23L,8L));
        assertStatus(HttpStatus.BAD_REQUEST,7L,23L,"COMPROBANTE",png());
    }
    @Test void subirPngValidoPersisteYNormalizaRuta() {
        usuarioActivo();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Archivo r=service.subir(7L,null," avatar ",archivo("C:\\tmp\\avatar.png","image/png",new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,1}));
        assertEquals("avatar.png",r.getNombreOriginal());
        assertEquals("AVATAR",r.getTipo());
        assertTrue(Files.exists(temporal.resolve(r.getNombreAlmacenado())));
    }
    @Test void subirPdfValidoComoComprobante() {
        usuarioActivo();
        when(pagos.buscarPorId(23L)).thenReturn(new PagosClient.PagoResumen(23L,7L));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Archivo r=service.subir(7L,23L,"comprobante",archivo("recibo.pdf","application/pdf",
            new byte[]{'%','P','D','F','-',1}));
        assertEquals("COMPROBANTE",r.getTipo());
        assertEquals(Long.valueOf(23L),r.getIdPago());
    }
    @Test void subirJpegValidoComoAdjunto() {
        usuarioActivo();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Archivo r=service.subir(7L,null,"adjunto",archivo("foto.jpeg","image/jpeg",
            new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF,1}));
        assertEquals("ADJUNTO",r.getTipo());
    }
    @Test void guardarFallaYEliminaArchivoFisico() {
        usuarioActivo();
        when(repo.save(any())).thenThrow(new IllegalStateException("DB no disponible"));
        assertThrows(IllegalStateException.class,()->service.subir(7L,null,"AVATAR",png()));
        try(var archivos=Files.list(temporal)) {
            assertEquals(0L,archivos.count());
        } catch(java.io.IOException e) { fail(e); }
    }
    @Test void listaPorPago() {
        when(repo.findByIdPago(23L)).thenReturn(List.of(new Archivo()));
        assertEquals(1,service.listarPorPago(23L).size());
    }
    @Test void listaTodos() {
        when(repo.findAll()).thenReturn(List.of(new Archivo()));
        assertEquals(1,service.listarTodos().size());
    }
    @Test void listaPorTipoNormalizado() {
        when(repo.findByTipo("COMPROBANTE")).thenReturn(List.of(new Archivo()));
        assertEquals(1,service.listarPorTipo(" comprobante ").size());
    }
    @Test void buscarInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> service.buscarPorId(9L)).getStatusCode());
    }
    @Test void descargarSinArchivoFisicoDa404() {
        Archivo a=new Archivo(); a.setActivo(true); a.setNombreAlmacenado("no-existe.png");
        when(repo.findById(9L)).thenReturn(Optional.of(a));
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> service.descargar(9L)).getStatusCode());
    }
    @Test void descargarArchivoPresente() throws Exception {
        Files.write(temporal.resolve("archivo.png"),new byte[]{1,2,3});
        Archivo a=new Archivo(); a.setActivo(true); a.setNombreAlmacenado("archivo.png");
        when(repo.findById(9L)).thenReturn(Optional.of(a));
        assertTrue(service.descargar(9L).exists());
    }
    @Test void descargarPrevieneSalidaDelDirectorioBase() {
        Archivo a=new Archivo(); a.setActivo(true); a.setNombreAlmacenado("../fuera.png");
        when(repo.findById(9L)).thenReturn(Optional.of(a));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> service.descargar(9L)).getStatusCode());
    }
    @Test void eliminarArchivoInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> service.eliminar(999L)).getStatusCode());
    }
    void assertStatus(HttpStatus esperado, Long usuario, Long pago, String tipo, MultipartFile archivo) {
        assertEquals(esperado,assertThrows(ResponseStatusException.class,
            () -> service.subir(usuario,pago,tipo,archivo)).getStatusCode());
        verify(repo,never()).save(any());
    }
}

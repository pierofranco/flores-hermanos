package pe.edu.flores.model;
import java.math.BigDecimal;
import java.time.*;
/** Entidades del modelo conceptual APF2. Persistencia mediante DAO JDBC. */
public final class Entidades {
 private Entidades(){}
 public record Agencia(long id,String nombre,String ciudad){}
 public record Empleado(long id,long agenciaId,String nombres,String apellidos,String cargo){}
 public record Pasajero(long id,String tipoDocumento,String documento,String nombres,String apellidos){}
 public record Ruta(long id,String origen,String destino,BigDecimal distanciaKm,int duracionMin){}
 public record Bus(long id,String placa,int capacidad,LocalDate soatHasta,LocalDate revisionHasta,String estado){}
 public record Conductor(long id,String nombres,String licencia,LocalDate vencimiento){}
 public record Asiento(long id,long busId,int numero,int piso,String tipo){}
 public record Viaje(long id,long rutaId,long busId,long conductorId,LocalDateTime salida,BigDecimal precio,String estado){}
 public record Promocion(long id,String codigo,String tipo,BigDecimal valor,LocalDate desde,LocalDate hasta){}
 public record CuentaFidelizacion(long id,long pasajeroId,int saldo,String nivel){}
 public record Boleto(long id,String codigo,String qr,long viajeId,long asientoId,long pasajeroId,BigDecimal precioFinal,String estado){}
 public record Pago(long id,long boletoId,String metodo,BigDecimal monto,String estado,boolean simulado){}
 public record MovimientoPuntos(long id,long cuentaId,long boletoId,String tipo,int puntos){}
 public record Manifiesto(long id,long viajeId,int totalPasajeros,String estadoEnvio){}
 public record Embarque(long id,long boletoId,long manifiestoId,long empleadoId,LocalDateTime fecha){}
}

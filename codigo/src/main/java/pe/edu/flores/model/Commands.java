package pe.edu.flores.model;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public final class Commands {
 private Commands(){}
 public record Reserva(@Positive long viajeId,@Positive long asientoId){}
 public record Venta(@Positive long viajeId,@Positive long asientoId,@Positive long pasajeroId,@NotBlank String token,String promocion,@Min(0) int puntos,@NotBlank String metodo,String operacion,@NotBlank String canal,boolean rechazarPago){}
 public record Embarque(@NotBlank String qr,@NotBlank String documento){}
 public record Reprogramacion(@Positive long viajeId,@Positive long asientoId,@NotBlank String token,@NotBlank String metodo,String operacion){}
 public record ResultadoVenta(long id,String codigo,String qr,BigDecimal total){}
}

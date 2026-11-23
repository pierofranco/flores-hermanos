package pe.edu.flores.integration;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import pe.edu.flores.service.BusinessException;
/** Adaptador académico: no se realizan cobros ni conexiones bancarias. */
@Component
public class DemoPaymentGateway implements PaymentGateway {
 @Override public Decision approve(String method,BigDecimal amount,String operation,boolean reject){if(reject)throw new BusinessException("Pago rechazado por el simulador. No se emitió ningún boleto");if(amount.signum()<0)throw new BusinessException("Importe de pago inválido");return new Decision("APROBADO",true);}
}

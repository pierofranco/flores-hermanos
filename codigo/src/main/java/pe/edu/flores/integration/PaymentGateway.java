package pe.edu.flores.integration;
import java.math.BigDecimal;
public interface PaymentGateway {
 record Decision(String estado,boolean simulado){}
 Decision approve(String method,BigDecimal amount,String operation,boolean reject);
}

package pe.edu.flores.integration;
import org.springframework.stereotype.Component;
/** Un resultado SIMULADO no acredita un envío oficial a SUTRAN. */
@Component
public class DemoManifestGateway implements ManifestGateway {
 @Override public Result send(long trip,boolean fail){return new Result(fail?"ERROR":"SIMULADO",fail?"DEMO-SIN-CONEXION":"DEMO-"+trip);}
}

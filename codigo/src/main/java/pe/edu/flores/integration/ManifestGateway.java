package pe.edu.flores.integration;
public interface ManifestGateway {
 record Result(String estado,String referencia){}
 Result send(long trip,boolean fail);
}

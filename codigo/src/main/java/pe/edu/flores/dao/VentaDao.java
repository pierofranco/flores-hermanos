package pe.edu.flores.dao;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class VentaDao {
 private final SqlDao db;
 public VentaDao(SqlDao db){this.db=db;}
 public Map<String,Object> lockSeat(long trip,long seat){return db.one("SELECT * FROM reserva WHERE viaje_id=? AND asiento_id=? FOR UPDATE",trip,seat);}
 public Map<String,Object> trip(long id){return db.one("SELECT v.*,r.origen,r.destino,r.distancia_km,r.duracion_min,b.placa,b.soat_hasta,b.revision_hasta,b.estado AS bus_estado,c.vencimiento AS licencia_hasta,c.activo AS conductor_activo FROM viaje v JOIN ruta r ON r.id=v.ruta_id JOIN bus b ON b.id=v.bus_id JOIN conductor c ON c.id=v.conductor_id WHERE v.id=?",id);}
 public List<Map<String,Object>> seats(long trip){return db.list("SELECT a.id,a.numero,a.piso,a.tipo,r.boleto_id,r.propietario,r.vence FROM reserva r JOIN asiento a ON a.id=r.asiento_id WHERE r.viaje_id=? ORDER BY a.numero",trip);}
 public Map<String,Object> ticket(long id){return db.one("SELECT b.*,p.tipo_documento,p.documento,p.nombres,p.apellidos,p.correo,v.salida,r.origen,r.destino,a.numero AS asiento,bu.placa,pa.metodo,pa.operacion,pa.simulado FROM boleto b JOIN pasajero p ON p.id=b.pasajero_id JOIN viaje v ON v.id=b.viaje_id JOIN ruta r ON r.id=v.ruta_id JOIN asiento a ON a.id=b.asiento_id JOIN bus bu ON bu.id=v.bus_id JOIN pago pa ON pa.boleto_id=b.id WHERE b.id=?",id);}
}

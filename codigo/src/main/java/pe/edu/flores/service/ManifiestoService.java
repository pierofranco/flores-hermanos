package pe.edu.flores.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.flores.dao.SqlDao;
@Service
public class ManifiestoService {
 private final SqlDao db;private final IdentityService identity;private final pe.edu.flores.integration.ManifestGateway gateway;
 public ManifiestoService(SqlDao db,IdentityService identity,pe.edu.flores.integration.ManifestGateway gateway){this.db=db;this.identity=identity;this.gateway=gateway;}
 @Transactional public Map<String,Object> generate(long trip){db.one("SELECT id FROM viaje WHERE id=? FOR UPDATE",trip);if(db.find("SELECT id FROM manifiesto WHERE viaje_id=?",trip).isEmpty())db.insert("INSERT INTO manifiesto(viaje_id,estado_envio) VALUES(?,'PENDIENTE')",trip);db.update("UPDATE manifiesto SET total_pasajeros=(SELECT COUNT(*) FROM embarque e JOIN boleto b ON b.id=e.boleto_id WHERE b.viaje_id=?) WHERE viaje_id=?",trip,trip);return detail(trip);}
 @Transactional(readOnly=true)public Map<String,Object> detail(long trip){var m=db.find("SELECT * FROM manifiesto WHERE viaje_id=?",trip).orElseGet(()->new LinkedHashMap<>(Map.of("viaje_id",trip,"estado_envio","SIN_GENERAR","total_pasajeros",0)));return Map.of("manifiesto",m,"pasajeros",db.list("SELECT b.codigo,p.tipo_documento,p.documento,p.nombres,p.apellidos,a.numero AS asiento,e.fecha FROM embarque e JOIN boleto b ON b.id=e.boleto_id JOIN pasajero p ON p.id=b.pasajero_id JOIN asiento a ON a.id=b.asiento_id WHERE b.viaje_id=? ORDER BY a.numero",trip));}
 @Transactional public Map<String,Object> send(long trip,boolean fail){generate(trip);var m=db.one("SELECT * FROM manifiesto WHERE viaje_id=? FOR UPDATE",trip);if(Set.of("SIMULADO","ENVIADO").contains(m.get("estado_envio")))return detail(trip);if(SqlDao.id(m,"total_pasajeros")==0)throw new BusinessException("No hay pasajeros embarcados para el manifiesto");var response=gateway.send(trip,fail);String state=response.estado();db.update("UPDATE manifiesto SET estado_envio=?,codigo_respuesta=?,intentos=intentos+1 WHERE id=?",state,response.referencia(),m.get("id"));db.audit(identity.username(),"MANIFIESTO",state+"; viaje="+trip+"; sin envío real a SUTRAN");return detail(trip);}
}

package pe.edu.flores.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.flores.dao.*;
import pe.edu.flores.model.Commands.Embarque;
@Service
public class EmbarqueService {
 private final SqlDao db;private final VentaDao trips;private final IdentityService identity;private final FidelizacionService points;
 public EmbarqueService(SqlDao db,VentaDao trips,IdentityService identity,FidelizacionService points){this.db=db;this.trips=trips;this.identity=identity;this.points=points;}
 @Transactional public Map<String,Object> board(long tripId,Embarque cmd){
  var trip=trips.trip(tripId);if(!Set.of("PROGRAMADO","EN_EMBARQUE").contains(trip.get("estado")))throw new BusinessException("El viaje no admite embarque");var ticket=db.find("SELECT * FROM boleto WHERE qr=? OR codigo=? FOR UPDATE",cmd.qr().trim(),cmd.qr().trim()).orElseThrow(()->new BusinessException("Boleto no encontrado"));if(SqlDao.id(ticket,"viaje_id")!=tripId)throw new BusinessException("El boleto pertenece a otro viaje");if(!"EMITIDO".equals(ticket.get("estado")))throw new BusinessException("Boleto anulado o ya utilizado");var passenger=db.one("SELECT * FROM pasajero WHERE id=?",ticket.get("pasajero_id"));if(!cmd.documento().trim().equals(passenger.get("documento")))throw new BusinessException("El documento no coincide con el pasajero");
  db.one("SELECT id FROM viaje WHERE id=? FOR UPDATE",tripId);
  var mf=db.find("SELECT * FROM manifiesto WHERE viaje_id=?",tripId);long manifest;
  if(mf.isEmpty())manifest=db.insert("INSERT INTO manifiesto(viaje_id,estado_envio) VALUES(?,'PENDIENTE')",tripId);else{if(Set.of("SIMULADO","ENVIADO").contains(mf.get().get("estado_envio")))throw new BusinessException("El manifiesto está cerrado; no admite nuevos embarques");manifest=SqlDao.id(mf.get(),"id");}
  db.update("INSERT INTO embarque(boleto_id,manifiesto_id,empleado_id) VALUES(?,?,?)",ticket.get("id"),manifest,identity.employee());db.update("UPDATE boleto SET estado='UTILIZADO' WHERE id=?",ticket.get("id"));db.update("UPDATE viaje SET estado='EN_EMBARQUE' WHERE id=?",tripId);int awarded=points.award(SqlDao.id(passenger,"id"),SqlDao.id(ticket,"id"),SqlDao.money(trip,"distancia_km"));db.update("UPDATE manifiesto SET total_pasajeros=(SELECT COUNT(*) FROM embarque WHERE manifiesto_id=?) WHERE id=?",manifest,manifest);db.audit(identity.username(),"EMBARQUE",ticket.get("codigo")+"; puntos="+awarded);return Map.of("boleto",ticket.get("codigo"),"estado","EMBARCADO","puntos",awarded,"manifiestoId",manifest);
 }
}

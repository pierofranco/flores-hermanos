package pe.edu.flores.service;
import java.math.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.flores.dao.SqlDao;
@Service
public class FidelizacionService {
 private final SqlDao db;private final IdentityService identity;
 @Value("${flores.points-km:10}") int km;
 @Value("${flores.point-value:0.10}") BigDecimal pointValue;
 public FidelizacionService(SqlDao db,IdentityService identity){this.db=db;this.identity=identity;}
 public Map<String,Object> account(long passenger,boolean lock){return db.one("SELECT * FROM cuenta_fidelizacion WHERE pasajero_id=?"+(lock?" FOR UPDATE":""),passenger);}
 public BigDecimal pointValue(){return pointValue;}
 public void redeem(long passenger,long ticket,int points){if(points==0)return;var a=account(passenger,true);if(points<0||SqlDao.id(a,"saldo")<points)throw new BusinessException("Saldo de puntos insuficiente");db.update("UPDATE cuenta_fidelizacion SET saldo=saldo-? WHERE id=?",points,a.get("id"));movement(a,ticket,"CANJEA",points);level(a.get("id"));}
 public int award(long passenger,long ticket,BigDecimal distance){int points=distance.divide(BigDecimal.valueOf(km),0,RoundingMode.DOWN).intValue();if(points>0){var a=account(passenger,true);db.update("UPDATE cuenta_fidelizacion SET saldo=saldo+? WHERE id=?",points,a.get("id"));movement(a,ticket,"ACUMULA",points);level(a.get("id"));}return points;}
 public void refund(long passenger,long ticket,int points){if(points<=0)return;var a=account(passenger,true);db.update("UPDATE cuenta_fidelizacion SET saldo=saldo+? WHERE id=?",points,a.get("id"));movement(a,ticket,"DEVOLUCION",points);level(a.get("id"));}
 private void movement(Map<String,Object>a,long t,String kind,int n){db.update("INSERT INTO movimiento_puntos(cuenta_id,boleto_id,tipo,puntos) VALUES(?,?,?,?)",a.get("id"),t,kind,n);}
 private void level(Object id){db.update("UPDATE cuenta_fidelizacion SET nivel=CASE WHEN saldo>=1000 THEN 'ORO' WHEN saldo>=500 THEN 'PLATA' ELSE 'BASICO' END WHERE id=?",id);}
 @Transactional(readOnly=true)public Map<String,Object> history(long passenger){identity.checkPassenger(passenger);return Map.of("cuenta",account(passenger,false),"movimientos",db.list("SELECT m.*,b.codigo FROM movimiento_puntos m LEFT JOIN boleto b ON b.id=m.boleto_id WHERE m.cuenta_id=? ORDER BY m.id DESC",account(passenger,false).get("id")),"valorPunto",pointValue,"kmPorPunto",km);}
}

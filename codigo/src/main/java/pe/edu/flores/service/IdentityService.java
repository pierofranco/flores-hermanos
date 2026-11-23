package pe.edu.flores.service;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.edu.flores.dao.SqlDao;
@Service
public class IdentityService {
 private final SqlDao db;
 public IdentityService(SqlDao db){this.db=db;}
 public String username(){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null)throw new BusinessException("Sesión requerida");return auth.getName();}
 public Map<String,Object> current(){return db.one("SELECT u.id,u.usuario,u.rol,u.empleado_id,u.pasajero_id,u.activo,e.agencia_id FROM usuario u LEFT JOIN empleado e ON e.id=u.empleado_id WHERE u.usuario=?",username());}
 public long employee(){Object id=current().get("empleado_id");if(id==null)throw new BusinessException("Se requiere un empleado autorizado");return ((Number)id).longValue();}
 public void checkPassenger(long id){var u=current();if("PASAJERO".equals(u.get("rol"))&&(!Objects.equals(id,((Number)u.get("pasajero_id")).longValue())))throw new org.springframework.security.access.AccessDeniedException("Solo puede consultar su propia cuenta");}
 public String role(){return current().get("rol").toString();}
}

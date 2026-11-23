package pe.edu.flores.config;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import pe.edu.flores.dao.SqlDao;
/** Revoca también las sesiones existentes al desactivar una cuenta o empleado. */
public class AccountStatusFilter extends OncePerRequestFilter {
 private final SqlDao db;
 public AccountStatusFilter(SqlDao db){this.db=db;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken)){
   var user=db.find("SELECT u.activo,e.activo AS empleado_activo FROM usuario u LEFT JOIN empleado e ON e.id=u.empleado_id WHERE u.usuario=?",auth.getName());
   if(user.isEmpty()||!Boolean.TRUE.equals(user.get().get("activo"))||Boolean.FALSE.equals(user.get().get("empleado_activo"))){SecurityContextHolder.clearContext();var session=req.getSession(false);if(session!=null)session.invalidate();res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"La cuenta o el empleado están desactivados\"}");return;}
  }
  chain.doFilter(req,res);
 }
}

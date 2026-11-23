package pe.edu.flores.dao;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
@Repository
public class SqlDao {
 private final JdbcTemplate jdbc;
 public SqlDao(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<Map<String,Object>> list(String sql,Object...args){return jdbc.query(sql,(rs,n)->{Map<String,Object> row=new LinkedHashMap<>();var md=rs.getMetaData();for(int i=1;i<=md.getColumnCount();i++)row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT),rs.getObject(i));return row;},args);}
 public Map<String,Object> one(String sql,Object...args){var rows=list(sql,args);if(rows.isEmpty())throw new NoSuchElementException("El registro no existe");return rows.get(0);}
 public Optional<Map<String,Object>> find(String sql,Object...args){var rows=list(sql,args);return rows.isEmpty()?Optional.empty():Optional.of(rows.get(0));}
 public int update(String sql,Object...args){return jdbc.update(sql,args);}
 public long insert(String sql,Object...args){var key=new GeneratedKeyHolder();jdbc.update(c->{var ps=c.prepareStatement(sql,new String[]{"id"});for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);return ps;},key);return Objects.requireNonNull(key.getKey()).longValue();}
 public long count(String sql,Object...args){return ((Number)one(sql,args).values().iterator().next()).longValue();}
 public void audit(String who,String action,String detail){update("INSERT INTO auditoria(usuario,accion,detalle) VALUES(?,?,?)",who,action,detail.length()>300?detail.substring(0,300):detail);}
 public JdbcTemplate jdbc(){return jdbc;}
 public static long id(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
 public static BigDecimal money(Map<String,Object> row,String key){return new BigDecimal(row.get(key).toString());}
 public static String text(Map<String,Object> row,String key){return Objects.toString(row.get(key),"");}
}

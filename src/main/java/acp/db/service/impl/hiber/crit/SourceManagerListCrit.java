package acp.db.service.impl.hiber.crit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import acp.db.domain.SourceClass;
import acp.db.service.impl.hiber.all.ManagerListHiber;
import acp.forms.dto.SourceDto;
import acp.utils.*;

public class SourceManagerListCrit extends ManagerListHiber<SourceDto> {
  private static Logger logger = LoggerFactory.getLogger(SourceManagerListCrit.class);

  private Class<?> tableClass; 
  private String[] fields;
  private String[] headers;
  private Class<?>[] types;
  
  private String pkColumn;
  private Long seqId;

  private String strAwhere;
  private String strWhere;

  private List<SourceDto> cacheObj = new ArrayList<>();

  public SourceManagerListCrit() {
    tableClass = SourceClass.class;
    
    fields = new String[] { "id", "name", "owner" };

    headers = new String[] { 
        "ID"
      , Messages.getString("Column.Name")
      , Messages.getString("Column.Owner") 
    };
    
    types = new Class<?>[] { 
        Long.class
      , String.class
      , String.class
    };

    pkColumn = fields[0];
    seqId = 1000L;

    strAwhere = null;
    strWhere = strAwhere;

    prepareQuery(null);
  }

  @Override
  public String[] getHeaders() {
    return headers;    
  }

  @Override
  public Class<?>[] getTypes() {
    return types;    
  }

  @Override
  public Long getSeqId() {
    return seqId;
  }

  @Override
  public void prepareQuery(Map<String,String> mapFilter) {
    if (mapFilter != null) {
      setWhere(mapFilter);
    } else {
      strWhere = strAwhere;
    }
  }

  private void setWhere(Map<String,String> mapFilter) {
    // ----------------------------------
    String vName = mapFilter.get("name"); 
    String vOwner = mapFilter.get("owner");;
    // ----------------------------------
    String phWhere = null;
    String str = null;
    // ---
    if (!QueryUtils.emptyString(vName)) {
      str = "upper(msss_name) like upper('" + vName + "%')";
      phWhere = QueryUtils.strAddAnd(phWhere, str);
    }
    // ---
    if (!QueryUtils.emptyString(vOwner)) {
      str = "upper(msss_owner) like upper('" + vOwner + "%')";
      phWhere = QueryUtils.strAddAnd(phWhere, str);
    }
    // ---
    strWhere = QueryUtils.strAddAnd(strAwhere, phWhere);
  }

  @Override
  public List<SourceDto> queryAll() {
    cacheObj = fetchPage(-1,-1);
    return cacheObj;    
  }

  @Override
  public List<SourceDto> fetchPage(int startPos, int cntRows) {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try { 
      // -------------------------------------
      ProjectionList pl = Projections.projectionList();
      for (int i=0; i<fields.length; i++) {
        pl.add(Property.forName(fields[i]));
      }
      // -----------
      Criteria query = session.createCriteria(tableClass);
      query.setProjection(pl);
      query.add(Restrictions.sqlRestriction(strWhere));
      query.addOrder(Order.asc(pkColumn));
      // -----------
      if (startPos>0) {
        query.setFirstResult(startPos-1);  // Hibernate начинает с 0
      }
      if (cntRows>0) {
        query.setMaxResults(cntRows);
      }  
      // ==============
      fillCache(query);
      // ==============
      tx.commit();
    } catch (HibernateException e) {
      tx.rollback();
      DialogUtils.errorPrint(e,logger);
    } catch (Exception e) {
      tx.rollback();
      DialogUtils.errorPrint(e,logger);
    } finally {
      session.close();
    }  
    return cacheObj;    
  }  


  private void fillCache(Criteria query) {
    // ============================
    List<?> objList = query.list();
    // ============================
    cacheObj = new ArrayList<>();
    for (int i=0; i < objList.size(); i++) {
      Object[] obj = (Object[]) objList.get(i);
      cacheObj.add(getObject(obj));
    }
  }
  
  private SourceDto getObject(Object[] obj) {
    //---------------------------------------
    Long rsId = (Long) obj[0];
    String rsName = (String) obj[1];
    String rsOwner = (String) obj[2];
    //---------------------------------------
    SourceDto objDto = new SourceDto();
    objDto.setId(rsId);
    objDto.setName(rsName);
    objDto.setOwner(rsOwner);
    //---------------------------------------
    return objDto;
  }

  @Override
  public long countRecords() {
    long cntRecords = 0; 
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      //-----------------------------------------------------------
      Criteria query = session.createCriteria(tableClass);
      // ---
      ProjectionList pl = Projections.projectionList();
      pl.add(Projections.rowCount());
      // ---
      query.setProjection(pl);
      query.add(Restrictions.sqlRestriction(strWhere));
      //-----------------------------------------------------------
      cntRecords = (Long) query.uniqueResult();
      tx.commit();
    } catch (HibernateException e) {
      tx.rollback();
      DialogUtils.errorPrint(e,logger);
    } catch (Exception e) {
      tx.rollback();
      DialogUtils.errorPrint(e,logger);
    } finally {
      session.close();
    }
    return cntRecords;    
  }

}

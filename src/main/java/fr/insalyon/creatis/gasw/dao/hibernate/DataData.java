package fr.insalyon.creatis.gasw.dao.hibernate;

import fr.insalyon.creatis.gasw.bean.Data;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DataDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DataData implements DataDAO {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Idempotent insert/update of a Data row keyed on data_path, to avoid
     * unique constraint violations when multiple threads parse output for
     * jobs referencing the same file path
     */
    @Override
    @Transactional
    public void upsertData(Data data) throws DAOException {
        try {
            entityManager
                    .createNamedQuery("Data.upsertData")
                    .setParameter(1, data.getDataPath())
                    .setParameter(2, data.getDataType().name())
                    .executeUpdate();
        } catch (HibernateException ex) {
            logger.error("Error while upserting data", ex);
            throw new DAOException(ex);
        }
    }
}

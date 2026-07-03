package fr.insalyon.creatis.gasw.dao;

import fr.insalyon.creatis.gasw.bean.Data;

public interface DataDAO {

    void upsertData(Data data) throws DAOException;

}

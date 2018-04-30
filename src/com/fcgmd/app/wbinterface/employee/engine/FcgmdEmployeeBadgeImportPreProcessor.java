package com.fcgmd.app.wbinterface.employee.engine;

import com.workbrain.app.wbinterface.table.TableTransaction;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLUtil;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;

public class FcgmdEmployeeBadgeImportPreProcessor extends TableTransaction {
    private static Logger logger = Logger.getLogger(FcgmdEmployeeBadgeImportPreProcessor.class);

    public FcgmdEmployeeBadgeImportPreProcessor() {
    }

    public void initializeTransaction(DBConnection conn) throws Exception {
        this.resetEmpBdgFlag1(conn);
        super.initializeTransaction(conn);
    }

    public void finalizeTransaction(DBConnection conn) throws Exception {
        PreparedStatement ps = null;
        String psDeleteSQL = "delete from EMPLOYEE_BADGE where empbdg_flag1 <> 'Y'";

        try {
            ps = conn.prepareStatement(psDeleteSQL);
            ps.executeUpdate();
            this.resetEmpBdgFlag1(conn);
        } finally {
            conn.commit();
            SQLUtil.cleanUp(ps);
        }

        super.finalizeTransaction(conn);
    }

    private void resetEmpBdgFlag1(DBConnection conn) throws Exception {
        PreparedStatement ps = null;
        String psUpdateFlagSQL = "update EMPLOYEE_BADGE set empbdg_flag1 = 'N'";

        try {
            ps = conn.prepareStatement(psUpdateFlagSQL);
            ps.executeUpdate();
        } finally {
            conn.commit();
            SQLUtil.cleanUp(ps);
        }

    }
}

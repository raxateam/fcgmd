package com.fcgmd.workbrain.app.modules.entitlements;

import com.workbrain.app.modules.entitlements.EntDetailData;
import com.workbrain.app.modules.entitlements.EntitlementData;
import com.workbrain.app.modules.entitlements.EntitlementException;
import com.workbrain.app.modules.entitlements.Ratio;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.ruleengine.WBData;
import java.sql.SQLException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class FcgmdEmpFteEntRatio implements Ratio {
    private static Logger logger = Logger.getLogger(FcgmdEmpFteEntRatio.class);

    public FcgmdEmpFteEntRatio() {
    }

    public double getRatio(WBData wbData, EntitlementData ent, EntDetailData entDetail) throws EntitlementException {
        double empFTE = 0.0D;

        try {
            EmployeeAccess ea = new EmployeeAccess(wbData.getDBconnection(), wbData.getCodeMapper());
            EmployeeData ed = ea.load(wbData.getEmpId(), wbData.getWrksWorkDate());
            empFTE = ed.getEmpFte();
        } catch (SQLException var8) {
            ;
        }

        double ratio = 1.0D;
        this.log("Ratio.getRatio()");
        return empFTE;
    }

    protected void log(String message) {
        if (logger.isEnabledFor(Level.DEBUG)) {
            logger.debug(message);
        }
    }
}

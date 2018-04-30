package com.fcgmd.app.ta.quickrules;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.app.ta.util.PayGroupHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringUtil;
import com.workbrain2.ta.domain.PayGroup;
import com.workbrain2.ta.domain.PayGroupType;
import com.workbrain2.ta.svc.payroll.PayGroupService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * Rule to initiate recalc for FLSA period. FLSA period start date is
 * configured through calc group udf1. FLSA period end date is calculated
 * based on the number of days configured in calcgrp_udf2.
 *
 */

public class FcgmdFlsaRecalcRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FcgmdFlsaRecalcRule.class);
    private static String WRKS_ERROR_MSG = "FLSA pattern start date and length are required to execute: FCGMD FLSA Recalculate Rule";

    public FcgmdFlsaRecalcRule() {
    }

    @Override
    public List<RuleParameterInfo> getParameterInfo(DBConnection conn) {
        List<RuleParameterInfo> ruleParamInfoList = new ArrayList<>();
        return ruleParamInfoList;
    }

    @Override
    public void execute(WBData wbData, Parameters parameters) throws Exception {

        PayGroupService pgs = PayGroupHelper.getPayGroupService();
        PayGroup pg = pgs.getPayGroup(wbData.getPaygrpId());
        PayGroupType pgt = pgs.getPayGroupType(pg.getPaygrptypId());

        //Get pay period duration
        int payPeriodDays = pgt.getPaygrptypDuration();

        //Get calc group UDF1 value
        CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();
        String calcGrpFlsaDateStr = codeMapper.getCalcGroupById(wbData.getRuleData().getEmployeeData().getCalcgrpId()).getCalcgrpUdf1();
		
		//Get calc group UDF2 value
        String calcGrpFlsaDays = codeMapper.getCalcGroupById(wbData.getRuleData().getEmployeeData().getCalcgrpId()).getCalcgrpUdf2();
        
		if(!StringUtil.isEmpty(calcGrpFlsaDateStr) && !StringUtil.isEmpty(calcGrpFlsaDays)){
			//Calculate the actual FLSA period start date
			Date flsaStartDate = DateHelper.convertStringToDate(calcGrpFlsaDateStr, "MM/dd/yyyy");

			//Calculate number of days in FLSA period
			int flsaPeriodDays = new Integer(calcGrpFlsaDays).intValue();
				
			//Calculate the day number in a 28 day FLSA period
			int flsaPeriodDay = DateHelper.getDifferenceInDays(wbData.getWrksWorkDate(), flsaStartDate) % flsaPeriodDays;

			if (flsaPeriodDay == 0) {//If it is 1st Day --> first pay period in FLSA period --> add 2nd flsa period dates
				for (int i = 0; i < payPeriodDays; i++) {
					wbData.addEmployeeDateToAutoRecalculate(wbData.getEmpId(), DateHelper.addDays(wbData.getWrksWorkDate(), i + payPeriodDays));
				}

			} else if (flsaPeriodDay == payPeriodDays) {//If it first day of second flsa period --> second pay period in FLSA period --> add 1st flsa period dates
				for (int i = 0; i < payPeriodDays; i++) {
					wbData.addEmployeeDateToAutoRecalculate(wbData.getEmpId(), DateHelper.addDays(wbData.getWrksWorkDate(), i - payPeriodDays));
				}
			}
		}else{
			wbData.setWrksError(WRKS_ERROR_MSG);
		}
    }

    @Override
    public String getComponentName() {
        return "FCGMD FLSA Recalculate Rule";
    }
}

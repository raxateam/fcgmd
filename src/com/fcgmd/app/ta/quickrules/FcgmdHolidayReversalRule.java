package com.fcgmd.app.ta.quickrules;

import java.util.ArrayList;
import java.util.List;

import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/*
 * A custom rule will be developed which reverses a minute of Holiday pay (HOL) for each minute of Holiday Leave Accrued.
 * 
 * @author Michael Mongiardi
 */

public class FcgmdHolidayReversalRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FcgmdHolidayReversalRule.class);

    public static final String PARAM_ELIGIBLE_TIME_CODES = "EligibleTimeCodes";
    public static final String PARAM_ELIGIBLE_HOUR_TYPES = "ElibigleHourTypes";
    public static final String PARAM_NEGATIVE_PREMIUM_TIME_CODE = "NegativePremiumTimeCode";
    
    public List<RuleParameterInfo> getParameterInfo(DBConnection conn) {
        List<RuleParameterInfo> result = new ArrayList<RuleParameterInfo>();

        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_TIME_CODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOUR_TYPES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_NEGATIVE_PREMIUM_TIME_CODE, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
    	   	
        ParametersResolved pars = new ParametersResolved();
        pars.eligibleTimeCodes =  parameters.getParameter(PARAM_ELIGIBLE_TIME_CODES, null);
        pars.eligibleHourTypes =  parameters.getParameter(PARAM_ELIGIBLE_HOUR_TYPES, null);
        pars.negativePremiumTimeCode =  parameters.getParameter(PARAM_NEGATIVE_PREMIUM_TIME_CODE, null);

		int eligibleMins = wbData.getMinutesWorkDetailPremiumRange(wbData.getWrksWorkDate(), wbData.getWrksWorkDate(), DateHelper.DATE_1900, DateHelper.DATE_3000, pars.eligibleTimeCodes, true, pars.eligibleHourTypes, true, null, false);    		

		if(eligibleMins > 0){
			wbData.insertWorkPremiumRecord( (-1)*eligibleMins, pars.negativePremiumTimeCode);
		}
    		
    }
    
    public String getComponentName() {
        return "FCMG Holiday Reversal Rule";
    }

    public class ParametersResolved {
        public String eligibleTimeCodes = null;
        public String eligibleHourTypes = null;
        public String negativePremiumTimeCode = null;
    }
}

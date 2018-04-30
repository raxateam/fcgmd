package com.fcgmd.app.ta.quickrules;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.workbrain.app.modules.budgeting.rules.RuleException;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.app.ta.util.PayGroupHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain2.ta.domain.PayPeriod;
import com.workbrain2.ta.svc.payroll.PayGroupService;

/*
 * A custom rule will be developed which evaluates the total amount of holiday an employee is
 * eligible to accrue in the period, and ensures the employee only accrues up to that amount.
 *
 * @author Michael Mongiardi
 */

public class FcgmdHolidayAccrualRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FcgmdHolidayAccrualRule.class);

    public static final String PARAM_ELIGIBLE_TIME_CODES = "EligibleTimeCodes";
    public static final String PARAM_ELIGIBLE_HOUR_TYPES = "ElibigleHourTypes";
    public static final String PARAM_PREMIUM_TIME_CODE = "PremiumTimeCode";

    public List<RuleParameterInfo> getParameterInfo(DBConnection conn) {
        List<RuleParameterInfo> result = new ArrayList<RuleParameterInfo>();

        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_TIME_CODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOUR_TYPES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIME_CODE, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws Exception {

        PayGroupService pgs = PayGroupHelper.getPayGroupService();
        PayPeriod pp = pgs.getPayPeriod(wbData.getPaygrpId(), wbData.getWrksWorkDate());

        // Get Params
        ParametersResolved pars = new ParametersResolved();
        pars.eligibleTimeCodes =  parameters.getParameter(PARAM_ELIGIBLE_TIME_CODES, null);
        pars.eligibleHourTypes =  parameters.getParameter(PARAM_ELIGIBLE_HOUR_TYPES, null);
        pars.premiumTimeCode =  parameters.getParameter(PARAM_PREMIUM_TIME_CODE, null);

        int numHolidays = getHolidaysInPayPeriod(pp, wbData);
        Double standardHours = 0.0;
        List<String> tcList = Arrays.asList((pars.eligibleTimeCodes).split(","));
        List<String> htList = Arrays.asList((pars.eligibleHourTypes).split(","));
        String premiumTimeCode = pars.premiumTimeCode;

        try{
            // Standard Hours are stored as EMP_VAL3
            standardHours = Double.parseDouble( wbData.getEmpVal3(wbData.getWrksWorkDate()) );
        }catch(Exception e){
            throw new RuleException("Please ensure to populate the employee has a valid Standard Hours value.",e);
        }

        //If there is at least one holiday in the current pay period
        if (numHolidays >= 1 ){
            // The premium should START paying at this many minutes (NumHolidays*(.2 *EMP_VAL3* 60))
            double premStart = (standardHours*120) - (numHolidays*standardHours*12);
            // The premium should STOP paying at this many minutes
            double premEnd = (standardHours*120);

            //How much of this premium has been taken this period
            int premTaken = wbData.getMinutesWorkDetailPremiumRange(pp.getPaypdStart(), wbData.getWrksWorkDate(), DateHelper.DATE_1900, DateHelper.DATE_3000, premiumTimeCode, true, null, true, WorkDetailData.PREMIUM_TYPE, false);

            int elibigleWorkedMins = 0;
            int maxTotalPremiumMins = (int)Math.round(premEnd-premStart);
            int premiumMinsToInsert = 0;

            // Set the number of eligible minutes (tcode/htype) that have ocurred so far
            if(!DateHelper.equals(pp.getPaypdStart(), wbData.getWrksWorkDate()))
                elibigleWorkedMins = wbData.getMinutesWorkDetailPremiumRange(pp.getPaypdStart(), DateHelper.addDays(wbData.getWrksWorkDate(),-1), DateHelper.DATE_1900, DateHelper.DATE_3000, pars.eligibleTimeCodes, true, pars.eligibleHourTypes, true, null, false);

            //Get work detail minutes for today
            WorkDetailList<WorkDetailData> wdl = wbData.getWorkDetails(wbData.getWrksWorkDate(), wbData.getWrksWorkDate(), WorkDetailData.DETAIL_TYPE);

            //Loop through today's minutes and add to eligible worked mins
            for (int i = 0; i < wdl.size(); i++){
                WorkDetailData wdd = wdl.get(i);
                // If Work Detail is eligible
                if (tcList.contains(wdd.getWrkdTcodeName()) && htList.contains(wdd.getWrkdHtypeName())){
                    elibigleWorkedMins = elibigleWorkedMins + wdd.getWrkdMinutes();
                }
            }

            if(elibigleWorkedMins > premStart && maxTotalPremiumMins > 0 ){
                premiumMinsToInsert = elibigleWorkedMins - (int)Math.round(premStart) - premTaken;
                if(premiumMinsToInsert > maxTotalPremiumMins){
                    premiumMinsToInsert = maxTotalPremiumMins - premTaken;
                }

                if(premiumMinsToInsert > 0){
                    wbData.insertWorkPremiumRecord(premiumMinsToInsert, premiumTimeCode);
                }
            }

        }
    }

    /*
     * Get the number of Holidays in the current Pay Period in question
     */
    public int getHolidaysInPayPeriod(PayPeriod pp, WBData wbData) throws SQLException{

        List<Long> empIdLst = new ArrayList<Long>();
        empIdLst.add(wbData.getEmpId());

        int holidaysInPeriod = 0;

        //Need to use Override Access as we need to look into the future to see if there are holidays there.
        OverrideAccess oa = new OverrideAccess(wbData.getDBconnection());
        OverrideList ol = oa.loadByRangeAndTypeEmps(empIdLst, pp.getPaypdStart(), pp.getPaypdEnd(), 900, 900);
        ol.sortByStartTime();

        if (null == ol || ol.size() == 0){
            return holidaysInPeriod;
        }

        List<Date> datesContainingHolidays = new ArrayList<Date>();

        for(int i = 0; i < ol.size(); i++){

            OverrideData od = ol.get(i);
            Date currDate = ol.get(i).getOvrStartDate();

            if( (od.getOvrStatus().equals(OverrideData.PENDING) || od.getOvrStatus().equals(OverrideData.APPLIED))
                    && null != datesContainingHolidays && !datesContainingHolidays.contains(currDate)){
                holidaysInPeriod ++;
                datesContainingHolidays.add(currDate);
            }

        }

        return holidaysInPeriod;

    }

    public String getComponentName() {
        return "FCMG Holiday Accrual Rule";
    }

    public class ParametersResolved {
        public String eligibleTimeCodes = null;
        public String eligibleHourTypes = null;
        public String premiumTimeCode = null;
    }
}

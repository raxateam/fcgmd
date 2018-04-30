package com.fcgmd.app.ta.quickrules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.workbrain.app.ta.db.EmployeeJobAccess;
import com.workbrain.app.ta.db.JobAccess;
import com.workbrain.app.ta.model.EmployeeJobData;
import com.workbrain.app.ta.model.JobData;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/*	
 * If the job in the work detail segment is defined as a non primary job for the employ-ee, 
 * the hour type of the work detail record will be changed from REG to NPR. 
 * A non-primary job is defined as any job where the employee job rank (EMPJOB_RANK) is not equal to 1.
 * 
 * @author Michael Mongiardi
 */

public class FcgmdSecondaryJobsRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FcgmdSecondaryJobsRule.class);

    public static final String PARAM_JOB_TIME_CODE = "JobTimeCode";
    public static final String PARAM_JOB_HOUR_TYPE = "JobHourType";
    public static final String PARAM_CHANGE_TO_TIME_CODE = "ChangeToTimeCode";

    
    public List<RuleParameterInfo> getParameterInfo(DBConnection conn) {
        List<RuleParameterInfo> result = new ArrayList<RuleParameterInfo>();

        result.add(new RuleParameterInfo(PARAM_JOB_TIME_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_JOB_HOUR_TYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_CHANGE_TO_TIME_CODE, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
    	   	
        ParametersResolved pars = new ParametersResolved();
        pars.jobTimeCode =  parameters.getParameter(PARAM_JOB_TIME_CODE, null);
        pars.jobHourType =  parameters.getParameter(PARAM_JOB_HOUR_TYPE, null);
        pars.changeToTimeCode =  parameters.getParameter(PARAM_CHANGE_TO_TIME_CODE, null);

        WorkDetailList<WorkDetailData> wdl = wbData.getWorkDetails(wbData.getWrksWorkDate(), wbData.getWrksWorkDate(), WorkDetailData.DETAIL_TYPE);
        
        List<String> tcList = Arrays.asList((pars.jobTimeCode).split(","));
        List<String> htList = Arrays.asList((pars.jobHourType).split(","));
        
        for (int i = 0; i < wdl.size(); i++){
        	
        	WorkDetailData wdd = wdl.get(i);
        	
        	if (tcList.contains(wdd.getWrkdTcodeName()) && htList.contains(wdd.getWrkdHtypeName())){
        		
        		List<EmployeeJobData> ejl = wbData.getEmployeeJobList();
        		        		
                for (int j = 0; j < ejl.size(); j++){
                	
                	EmployeeJobData ejd = ejl.get(j);

                	if(ejd.getEmpjobRank() != 1 && ejd.getJobId() == wdd.getJobId()){
                		wbData.setWorkDetailHtypeName(pars.changeToTimeCode, wdd.getWrkdStartTime(), wdd.getWrkdEndTime(), pars.jobTimeCode, false);
                	}                	
                }
        	}
        }
    }
    
    public String getComponentName() {
        return "FCMG Secondary Job Rule";
    }

    public class ParametersResolved {
        public String jobTimeCode = null;
        public String jobHourType = null;
        public String changeToTimeCode = null;
    }
}

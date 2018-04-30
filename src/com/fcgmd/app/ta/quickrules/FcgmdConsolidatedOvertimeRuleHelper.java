package com.fcgmd.app.ta.quickrules;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.EmployeeSchedDtlData;
import com.workbrain.app.ta.model.EmployeeSchedDtlList;
import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.RuleData;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.model.WorkSummaryData;
import com.workbrain.app.ta.quickrules.overtime.HourSetDescriptionProcessor;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Parm;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.util.DateHelper;
import com.workbrain.util.SimpleTimePeriod;
import com.workbrain.util.StringUtil;
import com.workbrain.util.TimePeriod;
import com.workbrain.util.TimePeriodHelper;

public class FcgmdConsolidatedOvertimeRuleHelper {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FcgmdConsolidatedOvertimeRuleHelper.class);

    /**
     * This is the main helper entry point for the overtime rule logic.
     * 
     * @param wbData  current day's calculation data
     * @param parameterData overtime rule configuration parameters
     * @throws Exception
     */
    public static void applyOvertimeRule(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData) throws Exception {
    	if(logger.isDebugEnabled()){
    		logInputParameters(parameterData);
    		logWorkDetails(wbData, parameterData, "IN");
    	}
    	
    	// apply OT rule to workdetails
    	applyOvertimeRuleToWorkDetails(wbData, parameterData);
        
        if(logger.isDebugEnabled()){
        	logWorkDetails(wbData, parameterData, "OUT");
        }
    }

    /**
     * This method applies overtime rule to work details.
     * 
     * @param wbData  employee calculation data
     * @param parameterData overtime rule parameters
     * @throws Exception
     */
	public static void applyOvertimeRuleToWorkDetails(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData) throws Exception {
    	if (wbData.getRuleData().getWorkDetailCount() <= 0) {
            // **** Nothing to apply the rule to ****
            return;
        }
        
    	/** Parameters that we are going to use during the calculation **/
        boolean codesInclusive = true;
        boolean applyFirstHourTypeToDetail = parameterData.isApplyFirstHourTypeInHourSetToDetail();
        boolean applyFirstHourTypeToPremium = parameterData.isApplyFirstHourTypeInHourSetToPremium();        
        
        /** parameters derived from the input **/
    	//DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);
        Parameters inputParsedHourSet = Parameters.parseParameters(parameterData.getHourSetDescription());
        
        Date inputInShiftStartTime = parameterData.getInShiftStartTime();
        Date inputInShiftEndTime   = parameterData.getInShiftEndTime();
        
        int inputSeedMinutes = parameterData.getSeedMinutes();
        
        Date inputIntervalStartTime = parameterData.getIntervalStartTime();
        Date inputIntervalEndTime   = parameterData.getIntervalEndTime();
        Date inputIntervalStartDate = parameterData.getIntervalStartDate();
        Date inputIntervalEndDate  = parameterData.getIntervalEndDate();
        
        String eligibleHourTypes = parameterData.getEligibleHourTypes();
        String eligibleTimecodes = parameterData.getEligibleWorkDetailTimeCodes();
        String protectedTimeCodes = parameterData.getProtectedWorkDetailTimeCodes();
        String protectedHourTypes = parameterData.getProtectedHourTypes();
        String resetTimeCodes = parameterData.getResetTimeCodesInCommaDelimitedString();
        
        boolean addPremium = !parameterData.isApplyOvertimeToDetails();
        boolean inputAssignBetterRate = parameterData.getAssignBetterHourType();
        
        // Do we have a situation where inshift times are specified?
        Date inshiftStartTime = new Date(inputInShiftStartTime == null ? wbData.getRuleData().getWorkDetail(0).getWrkdStartTime().getTime() : inputInShiftStartTime.getTime());
        Date inshiftEndTime   = new Date (inputInShiftEndTime == null ? wbData.getRuleData().getWorkDetail(wbData.getRuleData().getWorkDetailCount() - 1).getWrkdEndTime().getTime() : inputInShiftEndTime.getTime());
        
        // The following is the rule implementation logic
        Date minimumTime = new Date();
        int seedMinutes = 0;
        
        // Newly premiums to insert
        WorkDetailList<WorkDetailData> premiumsToInsert = new WorkDetailList<WorkDetailData>(); 
        
        // In ScheduleBased case, we calculate the complete seed value and pass it.
        // When that happens we don't need to calculate the seed again.
        if(!parameterData.isFullySeeded()){
	        seedMinutes += calculateMinutesOnlyFromEligibleTimeCodesForRange(wbData, parameterData, inputIntervalStartDate, inputIntervalEndDate, inputIntervalStartTime, inputIntervalEndTime, codesInclusive, eligibleHourTypes, codesInclusive);
	        
	        if (inputIntervalStartTime == null || inputIntervalEndTime == null) {
	            Date workSummaryDate = wbData.getRuleData().getWrksWorkDate();
	            Date startTime = null, endTime = null;
	            if (inputIntervalStartTime != null) {
	                startTime = DateHelper.setTimeValues(workSummaryDate, inputIntervalStartTime);
	            }
	            if (inputIntervalEndTime != null) {
	                endTime = DateHelper.setTimeValues(workSummaryDate, inputIntervalEndTime);
	            }
	            if (DateHelper.isBetween(workSummaryDate, inputIntervalStartDate, inputIntervalEndDate)) {
	            	seedMinutes -= calculateMinutesOnlyFromEligibleTimeCodesForRange(wbData, parameterData, workSummaryDate, workSummaryDate, startTime, endTime, codesInclusive, eligibleHourTypes, codesInclusive);
	                
	            	// no need to subtract the eligible premiums as they should be accounted before details
	            }
	        }
        }
        
        seedMinutes += inputSeedMinutes;
        wbData.pluck(inshiftStartTime, inshiftEndTime, codesInclusive, eligibleTimecodes, codesInclusive, eligibleHourTypes);
        if(protectedTimeCodes != null || protectedHourTypes != null){
        	wbData.pluck(inshiftStartTime, inshiftEndTime, protectedTimeCodes, protectedHourTypes);
        }
        if(eligibleTimecodes == null){
        	if(resetTimeCodes != null){
        		wbData.pluck(inshiftStartTime, inshiftEndTime, false, resetTimeCodes, true, null);
        	}        	
        }
        
        if(wbData.getRuleData().getWorkDetailCount() > 0){
        	minimumTime.setTime(inshiftStartTime.getTime());
        	
        	inshiftStartTime.setTime(inshiftStartTime.getTime() - seedMinutes * DateHelper.MINUTE_MILLISECODS);
        	//WorkDetailList<WorkDetailData> splitWorkDetailList = splitWorkDetailsByHourSetDurations(wbData, parameterData);
        
            Enumeration<Parm> enumValues = inputParsedHourSet.getParameters();
            int iHourSetIndex = 0;
            int last_int_minutes = 0;
            
            while (enumValues.hasMoreElements()){
            	String hourTypeName = (enumValues.nextElement())._name;
            	boolean uptoFlag = false;
            	String int_minutes_str = inputParsedHourSet.getParameter(hourTypeName);
              if (int_minutes_str.endsWith("*")) {
                  uptoFlag = true;
                  int_minutes_str = int_minutes_str.substring(0, int_minutes_str.length()-1);
              }
              int int_minutes = 0;
              if ( !StringUtil.isEmpty(int_minutes_str) ) {
                  int_minutes = Integer.parseInt( int_minutes_str );
              }

              if (int_minutes == 0) {
                  int_minutes = calculateScheduleDuration(wbData, parameterData);
              }
              int temp_int_minutes  = int_minutes;
              if (uptoFlag) {
                  int_minutes = int_minutes - last_int_minutes >= 0 ? int_minutes - last_int_minutes : 0;
                  last_int_minutes += int_minutes;
              }else{
            	  last_int_minutes += temp_int_minutes;
              }

              inshiftEndTime.setTime(inshiftStartTime.getTime() + int_minutes * DateHelper.MINUTE_MILLISECODS);
              if (inshiftEndTime.after(minimumTime)) {
                  Date maxStart = RuleHelper.max(inshiftStartTime, minimumTime);
                  Date minEnd = RuleHelper.min(inshiftEndTime, wbData.getMaxEndTime(null, false));
                  if (addPremium && (applyFirstHourTypeToPremium || iHourSetIndex !=0)) {
                      int minutes = RuleHelper.diffInMinutes(maxStart, minEnd);
                      if (minutes > 0) { 	  
                      		insertOvertimePremiumRecord(wbData, parameterData, minutes, hourTypeName, maxStart, minEnd, null, premiumsToInsert);
                      	// We have inserted a premium or premiums per detail,
                      	// we might have to mark the corresponding details with a special hour type
                        if(parameterData.getHourTypeForWorkDetailsWithOvertime() != null){
                        	wbData.setWorkDetailHtypeId(
                                    wbData.getRuleData().getCodeMapper().getHourTypeByName(parameterData.getHourTypeForWorkDetailsWithOvertime()).getHtypeId(),
                                    maxStart,minEnd,null,false);
                        }
                      }
                  } else {
                	  if((applyFirstHourTypeToDetail || iHourSetIndex !=0)){
	                	  wbData.setWorkDetailHtypeId(
	                              wbData.getRuleData().getCodeMapper().getHourTypeByName(hourTypeName).getHtypeId(),
	                              maxStart,minEnd,null,inputAssignBetterRate);
                	  }
                  }
              }
              inshiftStartTime.setTime(inshiftEndTime.getTime());
              iHourSetIndex++;
            }
        }
        
        if(premiumsToInsert.size() > 0){
        	if(parameterData.getPopulatePremiumTimeFields()){
        		unpluckPremiums(wbData, premiumsToInsert);
        	}
        	parameterData.addAllPremiumsToList(premiumsToInsert);
        }
        
        wbData.unpluck();
    }
    
	private static void unpluckPremiums(WBData wbData, WorkDetailList<WorkDetailData> premiums){
		List<WorkDetailData> pluckedDetails = wbData.getWorkDetailsPlucked();
		
		//Since plucked details are in reverse chronological order, start at end of the list
		for(int i = pluckedDetails.size()-1; i >= 0; i--){
			WorkDetailData plucked = pluckedDetails.get(i);
			
			//split premiums at the plucked detail's start time
			premiums.splitAt(plucked.getWrkdStartTime());
			
			//store loop invariant information
			Date pluckedStart = plucked.getWrkdStartTime();
			int offset = (int)DateHelper.getMinutesBetween(plucked.getWrkdEndTime(), pluckedStart);
			
			//Increase start and end time of premiums that starts on or after the plucked detail by offset number of minutes
			//to account for the plucked detail
			for(int j =0, jsize = premiums.size(); j < jsize; j++){
				WorkDetailData premium = premiums.get(j);
				if(premium.getWrkdStartTime().compareTo(plucked.getWrkdStartTime()) >= 0){
					premium.setWrkdStartTime(DateHelper.addMinutes(premium.getWrkdStartTime(), offset));
					premium.setWrkdEndTime(DateHelper.addMinutes(premium.getWrkdEndTime(), offset));
				}
			}
		}
	}
	
    private static int calculateScheduleDuration(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData) throws SQLException{
    	List<EmployeeSchedDtlData> schdList = null;
    	int totalMinutes = 0;
		if(parameterData.isFullySeeded()){
			// we already have the list of schedules
			schdList = parameterData.getScheduleDetailsList();
		}else{
			Date startDate = DateHelper.truncateToDays(parameterData.getZoneStartDate());
			Date endDate   = DateHelper.truncateToDays(parameterData.getZoneEndDate());
			Date date = startDate;
			schdList = new EmployeeSchedDtlList();
			while(date.compareTo(endDate) <= 0) {
				EmployeeScheduleData schdData = wbData.getEmployeeScheduleData(date);
	        	List<EmployeeSchedDtlData> schdDetails = wbData.getRuleData().getCalcDataCache().getEmployeeScheduleDetails(schdData.getEmpskdId(), wbData.getDBconnection());
	        	if(schdDetails != null){
	        		schdList.addAll(schdDetails);
	        	}
				date = DateHelper.addDays(date, 1);
			}
		}
		
		for(int i = 0; i < schdList.size(); i++){
			EmployeeSchedDtlData schdDtl = schdList.get(i);
			
			totalMinutes += schdDtl.getEschdMinutes();
		}
		
		return totalMinutes;
	}

	/**
     * This method meets the following requirements:
     *   - inserts premium time code when insert premiums are enabled
     *   - Hour Type for premiums is inserted if present or else uses the hour set description hour type
     *   - populates time fields of the premium is selected.
     *   
     * @param wbData calculation data
     * @param parameterData overtime rule data
     * @param minutes  duration to insert premium for
     * @param hourTypeName hour type for the premium
     * @param premiumStart start time of the premium if we enable specifying premiums times
     * @param premiumEnd end time of the premium if we enable specifying the premiums times
     * @param wrkDetail  the workdetail that is causing the premium
     * @param premiums	 WorkDetailList<WorkDetailData> of all new premiums, must not be null
     * @throws Exception
     */
    private static void insertOvertimePremiumRecord(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData, int minutes, String hourTypeName, Date premiumStart, Date premiumEnd, WorkDetailData wrkDetail, WorkDetailList<WorkDetailData> premiums) throws Exception {
    	RuleData ruleData = wbData.getRuleData();
    	// we need to get WorkSummaryData so that we can link the premium correctly
    	WorkSummaryData wsData = ruleData.getWorkSummary(); 
    	//ruleData.getCodeMapper().getHourTypeById()
    	WorkDetailData premium = WorkDetailData.create(DateHelper.DATE_1900, DateHelper.DATE_1900, ruleData.getEmpDefaultLabor(0), wsData, ruleData.getEmployeeData(), ruleData.getCodeMapper());
        
        // Add time fields to the premium
        if(parameterData.getPopulatePremiumTimeFields()){
        	if(wrkDetail != null){
        		premium.setWrkdStartTime(wrkDetail.getWrkdStartTime());
        		premium.setWrkdEndTime(wrkDetail.getWrkdEndTime());
        	}else{
        		premium.setWrkdStartTime(premiumStart);
        		premium.setWrkdEndTime(premiumEnd);
        	}
        }
        
        //HourTypeData htd = ruleData.getCodeMapper().getHourTypeById(premium.getHtypeId());
        //premium.setWrkdRate(htd.getHtypeMultiple() * ruleData.getEmployeeData().getEmpBaseRate());
        premium.setWrkdType(WorkDetailData.PREMIUM_TYPE);
        premium.setWrkdMinutes(minutes);
        premium.setWrkdTcodeName(parameterData.getPremiumTimeCodeForOvertime());
        premium.setWrkdHtypeName(hourTypeName);
        premiums.add(premium);
    }
    
    /**
     * Calculates seed minutes using the protected time codes and hour types.
     * The resultant seed includes both from work details and premiums.
     * 
     * @param wbData workbrain employee calculation data
     * @param parameterData overtime rule parameters
     * @param intervalStartDate
     * @param intervalEndDate
     * @param intervalStartTime
     * @param intervalEndTime
     * @return minutes
     * @throws Exception
     */
    public static int calculateSeedTimeFromProtectedTimecodesHourtypes(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData,
    		                     Date intervalStartDate, Date intervalEndDate,
    		                     Date intervalStartTime, Date intervalEndTime) throws Exception
    {
    	int seedTime = 0;

    	if((parameterData.getProtectedWorkDetailTimeCodes() != null) || (parameterData.getProtectedHourTypes() != null)){
    		seedTime += wbData.getMinutesWorkDetailRange(intervalStartDate, intervalEndDate, intervalStartTime, intervalEndTime,
    				parameterData.getProtectedWorkDetailTimeCodes(), true, parameterData.getProtectedHourTypes(), true);
    	}
    	
    	// add protected premium time code minutes alone
    	if((parameterData.getProtectedPremiumHourTypes() != null) || (parameterData.getProtectedPremiumTimeCodes() != null)){
    		seedTime += wbData.getMinutesWorkDetailPremiumRange(intervalStartDate, intervalEndDate, intervalStartTime, intervalEndTime,
    				    parameterData.getProtectedPremiumTimeCodes(), true, parameterData.getProtectedPremiumHourTypes(), true, "P", false);
    	}
    	
    	return seedTime;
    }
    
    /*
     * This method takes two strings having takens. If there is token in the first
     * string that is also present in the second, it will remove that token form the
     * first string. As a result the first string becomes normal to the second whereby
     * the two string won't have any duplicates.
     * 
     * We can use this method to eliminate duplicates in one hourTypes list string from another
     * hourTypes list string. We can do the same with timeCodes list strings.
     */
	public static String removeDuplicatesInList(String stringToNormalize, String referenceString)
	{
		String normalizedString = null;	
		StringTokenizer st = new StringTokenizer(stringToNormalize, ",");
		
		while(st.hasMoreTokens()){
			String code = st.nextToken().trim();
			// Instead of writing our own, let us rely on the existing.
			if(!RuleHelper.isCodeInList(referenceString, code)){
				if(normalizedString == null){
					normalizedString = code;
				}else{
					normalizedString = normalizedString + "," + code;
				}
			}
		}
		
		return normalizedString;
	}
	
	/**
	 * Calculates time duration correctly when eligible timecodes specified is null and
	 * protected time codes and reset time codes exist.
	 * 
	 * @param wbData
	 * @param parameterData
	 * @param startDate
	 * @param endDate
	 * @param startTime
	 * @param endTime
	 * @param timecodesInclusive
	 * @param eligibleHourTypes
	 * @param hoursInclusive
	 * @return
	 * @throws SQLException
	 */
	public static int calculateMinutesOnlyFromEligibleTimeCodesForRange(WBData wbData, 
			FcgmdConsolidatedOvertimeRuleParameterData parameterData, Date startDate, Date endDate, 
			Date startTime, Date endTime,  boolean timecodesInclusive, 
			String eligibleHourTypes, boolean hoursInclusive) throws SQLException{
		int minutes = wbData.getMinutesWorkDetailRange(startDate, endDate, startTime, endTime, 
				parameterData.getEligibleWorkDetailTimeCodes(), timecodesInclusive, eligibleHourTypes, hoursInclusive);
		if(parameterData.getProtectedWorkDetailTimeCodes() != null || parameterData.getProtectedHourTypes() != null){
			minutes -= wbData.getMinutesWorkDetailRange(startDate, endDate, startTime, endTime, 
					parameterData.getProtectedWorkDetailTimeCodes(), timecodesInclusive, parameterData.getProtectedHourTypes(), hoursInclusive);
		}
		if(parameterData.getEligibleWorkDetailTimeCodes() == null && parameterData.getResetTimeCodesInCommaDelimitedString() != null){
			minutes -= wbData.getMinutesWorkDetailRange(startDate, endDate, startTime, endTime, 
					parameterData.getResetTimeCodesInCommaDelimitedString(), timecodesInclusive, eligibleHourTypes, hoursInclusive);
		}	
		
		return minutes;
	}
	
	/**
	 * This method obtains work detail minutes in the current day that are eligible, but not
	 * protected and reset time codes exist.
	 * 
	 * @param wbData
	 * @param parameterData
	 * @param startDate
	 * @param endDate
	 * @param timecodesInclusive
	 * @param eligibleHourTypes
	 * @param hoursInclusive
	 * @return
	 * @throws SQLException
	 */
	public static int calculateMinutesOnlyFromEligibleTimeCodesFromDetails(WBData wbData, 
			FcgmdConsolidatedOvertimeRuleParameterData parameterData, Date startDate, Date endDate, 
			boolean timecodesInclusive, String eligibleHourTypes, boolean hoursInclusive) throws SQLException{
		int minutes = wbData.getMinutesWorkDetail(startDate, endDate, 
				parameterData.getEligibleWorkDetailTimeCodes(), timecodesInclusive, eligibleHourTypes, hoursInclusive, null);
		if(parameterData.getProtectedWorkDetailTimeCodes() != null || parameterData.getProtectedHourTypes() != null){
			minutes -= wbData.getMinutesWorkDetail(startDate, endDate, 
					parameterData.getProtectedWorkDetailTimeCodes(), timecodesInclusive, parameterData.getProtectedHourTypes(), hoursInclusive, null);
		}		
		if(parameterData.getEligibleWorkDetailTimeCodes() == null && parameterData.getResetTimeCodesInCommaDelimitedString() != null){
			minutes -= wbData.getMinutesWorkDetail(startDate, endDate, 
					parameterData.getResetTimeCodesInCommaDelimitedString(), timecodesInclusive, eligibleHourTypes, hoursInclusive, null);
		}		
		
		return minutes;
	}
	
	
	/**
	  * Performs labor allocation algorithm. Resulting premium list are stored back to parameterData 
	  * 
	  * @param wbData
	  * @param parameterData rule parameters
	  */
	 public static void applyPremiumAllocationByLaborMetrics(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData){
		 if(logger.isDebugEnabled()){
			 logger.debug("Allocating Premium Hours by Labor Metrics for empId: " + wbData.getEmpId());
		 }
		 
		 WorkDetailList<WorkDetailData> todaysPremiumsList = parameterData.getRuleCreatedPremiums();
		 if(todaysPremiumsList.size() <= 0){
			 // there are no premiums to allocate for today
			 return;
		 }
		 
		 // prepare the input data
		 boolean allocateByJob         = parameterData.isAllocateByJob();
		 boolean allocateByDepartment  = parameterData.isAllocateByDepartment();
		 boolean allocateByProject     = parameterData.isAllocateByProject();
		 boolean allocateByDocket      = parameterData.isAllocateByDocket();
		 boolean allocateByTeam        = parameterData.isAllocateByTeam();  //WbtId
		 boolean allocateByTimeCode    = parameterData.isAllocateByTimeCode();
		 boolean allocateByHourType    = parameterData.isAllocateByHourType();
		 
		 boolean insertPremiumJob        = false;
		 boolean insertPremiumDepartment = false;
		 boolean insertPremiumProject    = false;
		 boolean insertPremiumDocket     = false;
		 boolean insertPremiumQuantity   = false;
		 boolean insertPremiumTimeCode   = false;
		 
		 // use premium Lobor Metric only when the same is not used in allocation
		 if((!allocateByJob) &&(parameterData.getPremiumJob() != null)){
			 insertPremiumJob = true;
		 }
		 
		 if((!allocateByDepartment) &&(parameterData.getPremiumDepartment() != null)){
			 insertPremiumDepartment = true;
		 }
		 
		 if((!allocateByProject) &&(parameterData.getPremiumProject() != null)){
			 insertPremiumProject = true;
		 }
		 
		 if((!allocateByDocket) &&(parameterData.getPremiumDocket() != null)){
			 insertPremiumDocket = true;
		 }
		 
		 if((!allocateByTimeCode) &&(parameterData.getPremiumTimeCodeForOvertime() != null)){
			 insertPremiumTimeCode = true;
		 }		 
		 
		 if(parameterData.getPremiumQuantity() != null){
			 insertPremiumQuantity = true;
		 }
		 
		 // Determine if we need to run the allocation
		 boolean requiresAllocation =  allocateByJob || allocateByDepartment || allocateByProject || allocateByDocket || allocateByTeam || allocateByTimeCode || allocateByHourType;
		 boolean requiresPremiumLabor = insertPremiumJob || insertPremiumDepartment || insertPremiumProject || insertPremiumDocket || insertPremiumQuantity || insertPremiumTimeCode;
		 String  possiblePremiumHourTypes = HourSetDescriptionProcessor.retrievePossibleHourTypes(parameterData.getHourSetDescription(), !parameterData.isApplyFirstHourTypeInHourSetToPremium()); 
		 
		 if((possiblePremiumHourTypes == null) || (!requiresAllocation && !requiresPremiumLabor)){
			 // allocation not selected
			 return;
		 }
		 
		 if(requiresPremiumLabor && !requiresAllocation){
			 // no need to run the allocation
			 for(int i = 0; i < todaysPremiumsList.size(); i++){
				WorkDetailData pwd = todaysPremiumsList.getWorkDetail(i);
				
			 	CodeMapper codeMapper = wbData.getCodeMapper();
               //Insert the configured premium labor metrics
               if(insertPremiumJob){
               	pwd.setJobId(codeMapper.getJobByName(parameterData.getPremiumJob()).getJobId());
               }
      		 	if(insertPremiumDepartment){
      		 		pwd.setDeptId(codeMapper.getDepartmentByName(parameterData.getPremiumDepartment()).getDeptId());
      		 	}
      		 	if(insertPremiumProject){
      		 		pwd.setProjId(codeMapper.getProjectByName(parameterData.getPremiumProject()).getProjId());
      		 	}
      		 	if(insertPremiumDocket){
      		 		pwd.setDockId(codeMapper.getDocketByName(parameterData.getPremiumDocket()).getDockId());
      		 	}
      		 	if(insertPremiumTimeCode){
      		 		pwd.setTcodeId(codeMapper.getTimeCodeByName(parameterData.getPremiumTimeCodeForOvertime()).getTcodeId());
      		 	}      		 	
      		 	if(insertPremiumQuantity){
      		 		// real number
      		 		pwd.setWrkdQuantity(Double.parseDouble(parameterData.getPremiumQuantity()));
      		 	}
			 }
			 //no reason to continue any further
			 return;
		 }
		 
		 int totalMinutes = 0;
		 Hashtable<String, Integer> metricsMinutes = new Hashtable<String, Integer>();
		 Date intervalStart = parameterData.getIntervalStartDate();
		 Date intervalEnd = parameterData.getIntervalEndDate();
		   
		 WorkDetailList<WorkDetailData> wdl;
		 //Grab work details from current calculation if allocate for one work day at a time
		 if(parameterData.isAllocationForDaily()){
			 wdl = new WorkDetailList<WorkDetailData>();
			 wdl.addAll(wbData.getRuleData().getWorkDetails());
		 }
		 else{
			 wdl = wbData.getWorkDetails(intervalStart, intervalEnd, null, 0);
		 }
		 logger.debug("Start: " + intervalStart + " End:" + intervalEnd);
		 
		 if(wdl == null){
			 logger.debug("Found no work details for the dates: " + parameterData.getIntervalStartDate() + " - " + parameterData.getIntervalEndDate());
			 return;
		 }
		 
		 //using reset time code than remove work details that are not part of the current calculation period
		 if(parameterData.getResetTimeCodesInCommaDelimitedString() != null){
			 for (Iterator<WorkDetailData> it = wdl.iterator(); it.hasNext();){
				 WorkDetailData wd = it.next();
				 if(!DateHelper.isBetween(wd.getWrkdStartTime(), intervalStart, intervalEnd) || !DateHelper.isBetween(wd.getWrkdEndTime(), intervalStart, intervalEnd)){
					 it.remove();
				 }
			 }
		 }

		 logger.debug("performing labor metrics allocation algorithm");
		 for (int i = 0; i < wdl.size(); i++) {
           WorkDetailData wd = wdl.get(i);
           logger.debug("date: " + wd.getWrkdWorkDate() + "- tcode: " + wd.getTcodeId() + ", Hour Type:" + wd.getHtypeId() + ", Minutes:" + wd.getWrkdMinutes());
           
           // Eligible Hour Type
           if (isTimeCodeInEligibleList(parameterData, wd.getWrkdTcodeName()) && RuleHelper.isCodeInList(parameterData.getEligibleHourTypes(), wd.getWrkdHtypeName()) && wd.isTypeDetail()){
               //Build the key for the hash table
               StringBuffer keyBuffer = new StringBuffer();
               
               if (allocateByJob) {
                   keyBuffer.append(wd.getJobId()).append(',');               
               }
               if (allocateByDepartment){
                   keyBuffer.append(wd.getDeptId()).append(',');
               }
               if (allocateByProject) {
                   keyBuffer.append(wd.getProjId()).append(',');               
               }
               if (allocateByDocket){
                   keyBuffer.append(wd.getDockId()).append(',');               
               }
               if(allocateByTeam){
               		keyBuffer.append(wd.getWbtId()).append(',');
               }
               if (allocateByTimeCode) {
                   keyBuffer.append(wd.getTcodeId()).append(',');               
               }
               if (allocateByHourType) {
                   keyBuffer.append(wd.getHtypeId());               
               }
               
               String key = keyBuffer.toString();
                  
               if (metricsMinutes.get(key) == null) {
                   metricsMinutes.put(key, new Integer(wd.getWrkdMinutes()));
                   totalMinutes += wd.getWrkdMinutes();
                      
               } else {
                   int workedMinutes = (metricsMinutes.get(key)).intValue();
                   metricsMinutes.put(key, new Integer(wd.getWrkdMinutes() + workedMinutes));
                   totalMinutes += wd.getWrkdMinutes();                
               }
           }
       }
		 
		 parameterData.setRuleCreatedPremiums(new WorkDetailList<WorkDetailData>()); // clear out the preimums list in parameterData
		 
		 for(int i = 0; i < todaysPremiumsList.size(); i++){
			 WorkDetailData premiumDetail = todaysPremiumsList.getWorkDetail(i);
			 int premiumDuration = premiumDetail.getWrkdMinutes();
			 int duration = 0;
	    
	               
           // Allocate Hours
           Enumeration<String> keys = metricsMinutes.keys();
           while (keys.hasMoreElements()){
               String key = keys.nextElement();
               Integer metricMinutes = metricsMinutes.get(key);
               double metric = (double)metricMinutes.intValue() / totalMinutes;
               duration = (int)Math.round(premiumDuration * metric );
               //Insert Work Premium with selected Metrics and duration
               WorkDetailData wrkDetail = premiumDetail.duplicate();
               if (allocateByJob) {
                   wrkDetail.setJobId(Long.parseLong(key.substring(0,key.indexOf(','))));
                   key = key.substring(key.indexOf(',') + 1);
               }
               if (allocateByDepartment){
                   wrkDetail.setDeptId(Long.parseLong(key.substring(0, key.indexOf(','))));
                   key = key.substring(key.indexOf(',') + 1);
               }
               if (allocateByProject) {
                   wrkDetail.setProjId(Long.parseLong(key.substring(0,key.indexOf(','))));
                   key = key.substring(key.indexOf(',') + 1);
               }
               if (allocateByDocket){
                   wrkDetail.setDockId(Long.parseLong(key.substring(0,key.indexOf(','))));
                   key = key.substring(key.indexOf(',') + 1);
               }
               if (allocateByTeam){
                   wrkDetail.setWbtId(Long.parseLong(key.substring(0,key.indexOf(','))));
                   key = key.substring(key.indexOf(',') + 1);
               }
               if (allocateByTimeCode) {
                   wrkDetail.setTcodeId(Long.parseLong(key.substring(0,key.indexOf(','))));
                   key = key.substring(key.indexOf(',') + 1);
               }
               if (allocateByHourType) {
                   wrkDetail.setHtypeId(Long.parseLong(key));
               }
               
               CodeMapper codeMapper = wbData.getCodeMapper();
               //Insert the configured premium labor metrics
               if(insertPremiumJob){
               	wrkDetail.setJobId(codeMapper.getJobByName(parameterData.getPremiumJob()).getJobId());
               }
      		 	if(insertPremiumDepartment){
      		 		wrkDetail.setDeptId(codeMapper.getDepartmentByName(parameterData.getPremiumDepartment()).getDeptId());
      		 	}
      		 	if(insertPremiumProject){
      		 		wrkDetail.setProjId(codeMapper.getProjectByName(parameterData.getPremiumProject()).getProjId());
      		 	}
      		 	if(insertPremiumDocket){
      		 		wrkDetail.setDockId(codeMapper.getDocketByName(parameterData.getPremiumDocket()).getDockId());
      		 	}
      		 	if(insertPremiumTimeCode){
      		 		wrkDetail.setTcodeId(codeMapper.getTimeCodeByName(parameterData.getPremiumTimeCodeForOvertime()).getTcodeId());
      		 	}      		 	
      		 	if(insertPremiumQuantity){
      		 		// real number
      		 		wrkDetail.setWrkdQuantity(Double.parseDouble(parameterData.getPremiumQuantity()));
      		 	}
               
      		 	premiumDetail.setCodeMapper(wbData.getCodeMapper());
      		 	wrkDetail.setCodeMapper(wbData.getCodeMapper());
      		 	createAndSavePremium(wbData, parameterData, duration, wrkDetail);
               
      		 	logger.debug("Inserting Overtime premium with duration: " + duration);
           }       
		 }
	}	
		
	/**
	* When eligible time codes is null, an eligible time code one that is not
	* protected.
	* 
	* @param parameterData rule parameter data
	* @param tcodeName time code to check for
	* @return 
	*/
	private static boolean isTimeCodeInEligibleList(FcgmdConsolidatedOvertimeRuleParameterData parameterData, String tcodeName){
		if(parameterData.getEligibleWorkDetailTimeCodes() != null){
			return RuleHelper.isCodeInList(parameterData.getEligibleWorkDetailTimeCodes(), tcodeName);
		}else if(parameterData.getProtectedWorkDetailTimeCodes() != null){
			return !RuleHelper.isCodeInList(parameterData.getProtectedWorkDetailTimeCodes(), tcodeName);
		}else{
			// both are null
			return true;
		}
	}	

	private static void createAndSavePremium(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData, int minutes, WorkDetailData wrkDetail){
    	RuleData ruleData = wbData.getRuleData();
    	// we need to get WorkSummaryData so that we can link the premium correctly
    	WorkSummaryData wsData = ruleData.getWorkSummary(); 
    	//ruleData.getCodeMapper().getHourTypeById()
    	WorkDetailData premium = WorkDetailData.create(DateHelper.DATE_1900, DateHelper.DATE_1900, ruleData.getEmpDefaultLabor(0), wsData, ruleData.getEmployeeData(), ruleData.getCodeMapper());
        
        premium.setWrkdType(WorkDetailData.PREMIUM_TYPE);
        premium.setWrkdMinutes(minutes);
        premium.setWrkdTcodeName(wrkDetail.getWrkdTcodeName());
        premium.setWrkdHtypeName(wrkDetail.getWrkdHtypeName());
        premium.setWrkdJobName(wrkDetail.getWrkdJobName());
        premium.setWrkdDeptName(wrkDetail.getWrkdDeptName());
        premium.setWrkdProjName(wrkDetail.getWrkdProjName());
        premium.setWrkdDockName(wrkDetail.getWrkdDockName());
        premium.setWrkdQuantity(wrkDetail.getWrkdQuantity());
        premium.setWbtId(wrkDetail.getWbtId());
        parameterData.addPremiumToList(premium);
    }	
	 
	
	/**
	 * Determine time periods of a work day outside of given schedule details 
	 * @param wbData - calculation data where the work detail records of the current work day is retrieved from
	 * @param todaysSchedPeriods - a List of TimePeriod objects for periods in schedule
	 * @param parameterData - Rule paramater values
	 * @return a List of TimePeriod objects for periods of the work day that are not in todaysSchedPeriods
	 */
	public static List<TimePeriod> calculateOutsideSchedulePeriods(WBData wbData, List<? extends TimePeriod> todaysSchedPeriods, FcgmdConsolidatedOvertimeRuleParameterData parameterData) {
		 List<SimpleTimePeriod> detailsPeriods = new ArrayList<SimpleTimePeriod>();
		 
		 WorkDetailList<WorkDetailData> eligibleDetails = wbData.getRuleData().getWorkDetails();
		 for(int i = 0; i < eligibleDetails.size(); i++){
			 WorkDetailData wdData = eligibleDetails.getWorkDetail(i);
			 detailsPeriods.add(new SimpleTimePeriod(wdData.getWrkdStartTime(), wdData.getWrkdEndTime()));
		 }
		 
		 return TimePeriodHelper.remainder(todaysSchedPeriods, detailsPeriods);
	}	
	
	/**
	 * Determine time periods of a work day outside of Employee's home team.
	 * @param wbData - calculation data where the work detail records of the current work day is retrieved from
	 * @param todaysSchedPeriods - a List of TimePeriod objects for WorkDetails.
	 * @param parameterData - Rule parameter values
	 * @param homeTeams ;List of employee's Home teams
	 * @return a List of TimePeriod objects for periods of the work day that are not in Home Team
	 */
	public static List<SimpleTimePeriod> calculateWDOutsideHomeTeam(WBData wbData, List<? extends TimePeriod> todayWDPeriods ,FcgmdConsolidatedOvertimeRuleParameterData parameterData ,List<Long> homeTeams) {
		 List<SimpleTimePeriod> detailsPeriodsOutsidehome = new ArrayList<SimpleTimePeriod>();
		 
		 WorkDetailList<WorkDetailData> eligibleDetails = wbData.getRuleData().getWorkDetails();
		 for(int i = 0; i < eligibleDetails.size(); i++){
			 WorkDetailData wdData = eligibleDetails.getWorkDetail(i);
			 if(!homeTeams.contains(wdData.getWbtId())){
				 detailsPeriodsOutsidehome.add(new SimpleTimePeriod(wdData.getWrkdStartTime(), wdData.getWrkdEndTime()));
			 }
		 }
		 return detailsPeriodsOutsidehome;
	}	
	
	public static List<SimpleTimePeriod> getTimePeriodsInsideHomeTeam(WBData wbData, List<? extends TimePeriod> todayWDPeriods ,FcgmdConsolidatedOvertimeRuleParameterData parameterData ,List<Long> homeTeams) {
		 List<SimpleTimePeriod> detailsPeriodsInsidehome = new ArrayList<SimpleTimePeriod>();
		 
		 WorkDetailList<WorkDetailData> eligibleDetails = wbData.getRuleData().getWorkDetails();
		 for(int i = 0; i < eligibleDetails.size(); i++){
			 WorkDetailData wdData = eligibleDetails.getWorkDetail(i);
			 if(homeTeams.contains(wdData.getWbtId())){
				 detailsPeriodsInsidehome.add(new SimpleTimePeriod(wdData.getWrkdStartTime(), wdData.getWrkdEndTime()));
			 }
		 }
		 for(int  i=0;i<detailsPeriodsInsidehome.size();i++){
			 
		 }
		 return detailsPeriodsInsidehome;
	}	
	
	
	/**
     * Merges adjacent employee schedule details into a List of contiguous SimpleTimePeriod(s).
     * In most cases, the resulting list will contain less number of items than the schedule details
	 * @param empSchDetails - a List of EmployeeSchedDtlData objects
	 * @return a List of SimpleTimePeriod objects
	 */
	public static List<SimpleTimePeriod> getMergedScheduleDetailsAsPeriods(List<EmployeeSchedDtlData> empSchDetails) {
		int lastEntryIndex = -1;
        List<SimpleTimePeriod> actSchTimePeriods = new ArrayList<SimpleTimePeriod>();
        for(int i = 0, size = empSchDetails.size(); i < size; i++){
        	EmployeeSchedDtlData schDtlData = empSchDetails.get(i);
        	if(lastEntryIndex > -1){
        		SimpleTimePeriod previousPeriod = actSchTimePeriods.get(lastEntryIndex);
        		if(previousPeriod.retrieveEndDate().equals(schDtlData.getEschdStartTime())){
        			actSchTimePeriods.set(lastEntryIndex, new SimpleTimePeriod(previousPeriod.retrieveStartDate(),schDtlData.getEschdEndTime()));
        		}
        		else{
        			actSchTimePeriods.add(new SimpleTimePeriod(schDtlData.getEschdStartTime(),schDtlData.getEschdEndTime()));
        			lastEntryIndex++;
        		}
        	}
        	else{
        		actSchTimePeriods.add(new SimpleTimePeriod(schDtlData.getEschdStartTime(),schDtlData.getEschdEndTime()));
        		lastEntryIndex++;
        	}
        }
        return actSchTimePeriods;
	}	
	
	/**
     * Merges adjacent employee work details into a List of contiguous SimpleTimePeriod(s).
     * In most cases, the resulting list will contain less number of items than the schedule details
	 * @param empWrkDetails - a List of WorkDetailData objects
	 * @return a List of SimpleTimePeriod objects
	 */
	public static List<SimpleTimePeriod> getMergedWrkDetailsAsPeriods(WorkDetailList<WorkDetailData> empWrkDetails) {
		int lastEntryIndex = -1;
        List<SimpleTimePeriod> actWDTimePeriods = new ArrayList<SimpleTimePeriod>();
        for(int i = 0, size = empWrkDetails.size(); i < size; i++){
        	WorkDetailData wrkData = empWrkDetails.get(i);
        	if(lastEntryIndex > -1){
        		SimpleTimePeriod previousPeriod = actWDTimePeriods.get(lastEntryIndex);
        		if(previousPeriod.retrieveEndDate().equals(wrkData.getWrkdStartTime())){
        			actWDTimePeriods.set(lastEntryIndex, new SimpleTimePeriod(previousPeriod.retrieveStartDate(),wrkData.getWrkdEndTime()));
        		}
        		else{
        			actWDTimePeriods.add(new SimpleTimePeriod(wrkData.getWrkdStartTime(),wrkData.getWrkdEndTime()));
        			lastEntryIndex++;
        		}
        	}
        	else{
        		actWDTimePeriods.add(new SimpleTimePeriod(wrkData.getWrkdStartTime(),wrkData.getWrkdEndTime()));
        		lastEntryIndex++;
        	}
        }
        return actWDTimePeriods;
	}	
	
	/**
	 * Combine permiums with the same labor metrics and rate. Premium minutes is the sum of all minutes from 
     * premiums combined. 
	 * @param premiumsList a List of WorkDetailData objects
	 * @return a List of WorkDetailData objects
	 */
	public static WorkDetailList<WorkDetailData> mergeSimilarPremiums(WorkDetailList<WorkDetailData> premiumsList) {
		WorkDetailList<WorkDetailData> mergedPremiums = new WorkDetailList<WorkDetailData>();
	    	
	    for(int i =0, size = premiumsList.size(); i < size; i++){
	    	boolean merge = false;
	    	WorkDetailData toMerge = premiumsList.getWorkDetail(i);
	    	for(int j =0, msize = mergedPremiums.size(); j < msize; j++){
	    		WorkDetailData candidate = mergedPremiums.getWorkDetail(j);
	    		
	    		merge =  candidate.getWrkdRate() == toMerge.getWrkdRate() &&
	    		         candidate.getTcodeId() == toMerge.getTcodeId() &&
	    		         candidate.getHtypeId() == toMerge.getHtypeId() &&
	    		         candidate.getJobId() == toMerge.getJobId() &&
	    		         candidate.getDockId() == toMerge.getDockId() &&
	    		         candidate.getProjId() == toMerge.getProjId() &&
	    		         candidate.getWbtId() ==  toMerge.getWbtId() &&
	    		         candidate.getDeptId() == toMerge.getDeptId() &&
	    		         candidate.getWrkdQuantity() == toMerge.getWrkdQuantity() &&
	    				 candidate.getWrkdStartTime().equals(toMerge.getWrkdStartTime()) &&
	    				 candidate.getWrkdEndTime().equals(toMerge.getWrkdEndTime());

		   		if(merge){
		   			candidate.setWrkdMinutes(candidate.getWrkdMinutes() + toMerge.getWrkdMinutes());
		   			break;
		   		}
	    	}
	    		
	    	if(!merge){
	    		mergedPremiums.add(toMerge);
	    	}
	    }
	    return mergedPremiums;
	}	
	
	/**
	 * Support method for logging rule parameters
	 * @param parameterData
	 */
	private static void logInputParameters(FcgmdConsolidatedOvertimeRuleParameterData parameterData) {
		// We should move this to ParameterData class...
		logger.debug("HourSet: " + parameterData.getHourSetDescription());
		logger.debug("EligibleHourType: " + parameterData.getEligibleHourTypes());
		logger.debug("EligibleTimeCodes: " + parameterData.getEligibleWorkDetailTimeCodes());
		logger.debug("ProtectedHourType: " + parameterData.getProtectedHourTypes());
		logger.debug("ProtectedTimeCodes: " + parameterData.getProtectedPremiumTimeCodes());
		logger.debug("Period: " + parameterData.getOvertimePeriod());
		logger.debug("Schedule Based: " + parameterData.isOvertimeAllocationScheduleBased());
		logger.debug("Premium Time Code: " +parameterData.getPremiumTimeCodeForOvertime());
		logger.debug("input seed: " + parameterData.getSeedMinutes());
		
	}
	
	/**
	 * Support method for logging the work details.
	 * 
	 * @param wbData
	 * @param parameterData
	 * @param inOrOut
	 */
	private static void logWorkDetails(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData, String inOrOut){
		Date startTime = parameterData.getIntervalStartDate();
		Date endTime   = parameterData.getIntervalEndDate();
		
		if(startTime == null){ startTime = parameterData.getZoneStartDate();}
		if(endTime  == null) { endTime = parameterData.getZoneEndDate();}
		
		WorkDetailList<WorkDetailData> wdList = wbData.getWorkDetails(startTime, endTime, WorkDetailData.DETAIL_TYPE);
		logger.debug( inOrOut + " : " + startTime + " - " + endTime);
		logger.debug("=========================BEGIN=================================");
		logger.debug(inOrOut + " : " + wdList.toDescription());
		wdList = wbData.getWorkDetails(startTime, endTime, WorkDetailData.PREMIUM_TYPE);
		logger.debug(inOrOut + " : " + wdList.toDescription());
		logger.debug("=========================END=================================");
	}
}
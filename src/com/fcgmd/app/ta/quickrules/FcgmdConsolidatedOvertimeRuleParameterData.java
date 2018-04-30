package com.fcgmd.app.ta.quickrules;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.ta.model.EmployeeSchedDtlData;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;

/**
 * This value object is the principle container for the
 * Consolidated Weekly Overtime Rule parameters;
 * included here both rule input and operational parameters.
 */
public class FcgmdConsolidatedOvertimeRuleParameterData {
    //hour set
    private String hourSetDescription;
    
    // time code and hour types
    private String eligibleWorkDetailTimeCodes;
    private String protectedWorkDetailTimeCodes;
    private String eligibleHourTypes;
    private String protectedHourTypes;
    private String protectedPremiumTimeCodes;
    private String protectedPremiumHourTypes;
    
    // apply Overtime To details    
    private boolean applyOvertimeToDetails = true;
    private boolean useFirstHalfOfCycle = true;
    
    // apply first hour type in hour set descrption to premiums
    private boolean applyFirstHourTypeInHourSetToPremium = false;
    // apply first hour type in hour set descrption to details
    private boolean applyFirstHourTypeInHourSetToDetail = false;    
    
    // overtime reset time code name and duration map (<String>,<Integer>)
    private HashMap<String, Integer> resetTimeCodeAndDuration = new HashMap<String, Integer>();
    private String resetTimeCodeString = null;
    
    //allocate by a single work day at a time
    private boolean isAllocationForDaily = false;
    
    // interval
    private String overtimePeriod;
    
    // for weekly only
    private String dayWeekStarts;
    
    private boolean overtimeAllocationScheduleBased;
    private boolean assignBetterHourType;
    
    private boolean splittingEnabled;
    private String periodThresholdTime;
    private boolean useEmployeeDayStartTime;
    private boolean overtimeStartDayPrevious;
    private boolean updateWorkdetailDateForThreshold;
    
    private String premiumTimeCodeForOvertime;
    //private String premiumHourTypeForOvertime;
    private String hourTypeForWorkDetailsWithOvertime;
    private boolean populatePremiumTimeFields;
    
    private boolean allocateByJob;
    private boolean allocateByDepartment;
    private boolean allocateByProject;
    private boolean allocateByDocket;
    private boolean allocateByTeam;
    private boolean allocateByTimeCode;
    private boolean allocateByHourType;
   
    private String premiumJob;
    private String premiumDepartment;
    private String premiumProject;
    private String premiumDocket;
    private String premiumQuantity;
    
    // functional parameters
    private Date   inShiftStartTime;
    private Date   inShiftEndTime;
    
    private Date   intervalStartTime;
    private Date   intervalEndTime;
    private Date   intervalStartDate;
    private Date   intervalEndDate;
    private Date   zoneStartDate;
    private Date   zoneEndDate;
    
    private int    seedMinutes;
    
    private boolean fullySeeded;
    
    private List<EmployeeSchedDtlData> scheduleDetailsList = null;
    
    private WorkDetailList<WorkDetailData> ruleCreatedPremiums = new WorkDetailList<WorkDetailData>();
    
    private boolean overtimeAllocationHomeTeamBased;
    private boolean useLooseHomeTeamLogic;
    
	public boolean isUseLooseHomeTeamLogic() {
		return useLooseHomeTeamLogic;
	}
	public void setUseLooseHomeTeamLogic(boolean useLooseHomeTeamLogic) {
		this.useLooseHomeTeamLogic = useLooseHomeTeamLogic;
	}
	public boolean isOvertimeAllocationHomeTeamBased() {
		return overtimeAllocationHomeTeamBased;
	}
	public boolean isUseFirstHalfOfCycle() {
		return useFirstHalfOfCycle;
	}
	public void setOvertimeAllocationHomeTeamBased(boolean allocateToNonHomeTeam) {
		this.overtimeAllocationHomeTeamBased=allocateToNonHomeTeam;
		
	}
	public boolean isAllocateByDepartment() {
		return allocateByDepartment;
	}
	public void setAllocateByDepartment(boolean allocateByDepartment) {
		this.allocateByDepartment = allocateByDepartment;
	}
	public boolean isAllocateByDocket() {
		return allocateByDocket;
	}
	public void setAllocateByDocket(boolean allocateByDocket) {
		this.allocateByDocket = allocateByDocket;
	}
	public boolean isAllocateByHourType() {
		return allocateByHourType;
	}
	public void setAllocateByHourType(boolean allocateByHourType) {
		this.allocateByHourType = allocateByHourType;
	}
	public boolean isAllocateByJob() {
		return allocateByJob;
	}
	public void setAllocateByJob(boolean allocateByJob) {
		this.allocateByJob = allocateByJob;
	}
	public boolean isAllocateByProject() {
		return allocateByProject;
	}
	public void setAllocateByProject(boolean allocateByProject) {
		this.allocateByProject = allocateByProject;
	}
	
	public boolean isAllocateByTimeCode() {
		return allocateByTimeCode;
	}
	public void setAllocateByTimeCode(boolean allocateByTimeCode) {
		this.allocateByTimeCode = allocateByTimeCode;
	}
	public boolean isOvertimeAllocationScheduleBased() {
		return overtimeAllocationScheduleBased;
	}
	public void setOvertimeAllocationScheduleBased(boolean scheduleBased) {
		this.overtimeAllocationScheduleBased = scheduleBased;
	}
	public boolean getAssignBetterHourType() {
		return assignBetterHourType;
	}
	public void setAssignBetterHourType(boolean assignBetterRate) {
		this.assignBetterHourType = assignBetterRate;
	}
	public String getPeriodThresholdTime() {
		return periodThresholdTime;
	}
	public void setPeriodThresholdTime(String dayStartTime) {
		this.periodThresholdTime = dayStartTime;
	}
	public String getDayWeekStarts() {
		return dayWeekStarts;
	}
	public void setDayWeekStarts(String dayWeekStarts) {
		this.dayWeekStarts = dayWeekStarts;
	}
	public String getOvertimePeriod() {
		return overtimePeriod;
	}
	public void setOvertimePeriod(String eligibleOvertimePeriod) {
		this.overtimePeriod = eligibleOvertimePeriod;
	}
	public String getHourSetDescription() {
		return hourSetDescription;
	}
	public void setHourSetDescription(String hourSetDescription) {
		this.hourSetDescription = hourSetDescription;
	}
	public String getEligibleHourTypes() {
		return eligibleHourTypes;
	}
	public void setEligibleHourTypes(String hourTypesCounted) {
		this.eligibleHourTypes = hourTypesCounted;
	}
	public String getProtectedHourTypes() {
		return protectedHourTypes;
	}
	public void setProtectedHourTypes(String hourTypesDiscounted) {
		this.protectedHourTypes = hourTypesDiscounted;
	}
	public boolean isOvertimeStartDayPrevious() {
		return overtimeStartDayPrevious;
	}
	public void setOvertimeStartDayPrevious(boolean overtimeStartDayPrevious) {
		this.overtimeStartDayPrevious = overtimeStartDayPrevious;
	}
	public boolean getPopulatePremiumTimeFields() {
		return populatePremiumTimeFields;
	}
	public void setPopulatePremiumTimeFields(boolean populatePremiumTimeFields) {
		this.populatePremiumTimeFields = populatePremiumTimeFields;
	}
	public String getPremiumDepartment() {
		return premiumDepartment;
	}
	public void setPremiumDepartment(String premiumDepartment) {
		this.premiumDepartment = premiumDepartment;
	}
	public String getPremiumDocket() {
		return premiumDocket;
	}
	public void setPremiumDocket(String premiumDocket) {
		this.premiumDocket = premiumDocket;
	}
	public String getPremiumJob() {
		return premiumJob;
	}
	public void setPremiumJob(String premiumJob) {
		this.premiumJob = premiumJob;
	}
	
	public String getPremiumProject() {
		return premiumProject;
	}
	public void setPremiumProject(String premiumProject) {
		this.premiumProject = premiumProject;
	}
	public String getPremiumQuantity() {
		return premiumQuantity;
	}
	public void setPremiumQuantity(String premiumQuantity) {
		this.premiumQuantity = premiumQuantity;
	}
	public String getPremiumTimeCodeForOvertime() {
		return premiumTimeCodeForOvertime;
	}
	public void setPremiumTimeCodeForOvertime(String premiumTimeCodeForOvertime) {
		this.premiumTimeCodeForOvertime = premiumTimeCodeForOvertime;
	}
	
	public String getProtectedPremiumTimeCodes() {
		return protectedPremiumTimeCodes;
	}
	public void setProtectedPremiumTimeCodes(String protectedEligiblePremiumTimeCodes) {
		this.protectedPremiumTimeCodes = protectedEligiblePremiumTimeCodes;
	}
	public boolean isUseEmployeeDayStartTime() {
		return useEmployeeDayStartTime;
	}
	public void setUseEmployeeDayStartTime(boolean useEmployeeDayStartTime) {
		this.useEmployeeDayStartTime = useEmployeeDayStartTime;
	}
	public String getEligibleWorkDetailTimeCodes() {
		return eligibleWorkDetailTimeCodes;
	}
	public void setEligibleWorkDetailTimeCodes(String workDetailTimeCodesCounted) {
		this.eligibleWorkDetailTimeCodes = workDetailTimeCodesCounted;
	}
	public String getProtectedWorkDetailTimeCodes() {
		return protectedWorkDetailTimeCodes;
	}
	public void setProtectedWorkDetailTimeCodes(
			String workDetailTimeCodesDiscounted) {
		this.protectedWorkDetailTimeCodes = workDetailTimeCodesDiscounted;
	}
	
	public void setInShiftStartTime(Date inShiftStartTime){
		this.inShiftStartTime = inShiftStartTime;
	}
	
	public Date getInShiftStartTime(){
		return this.inShiftStartTime;
	}
	
	public void setInShiftEndTime(Date inShiftEndTime){
		this.inShiftEndTime = inShiftEndTime;
	}
	
	public Date getInShiftEndTime(){
		return this.inShiftEndTime;
	}
	public Date getIntervalEndDate() {
		return intervalEndDate;
	}
	public void setIntervalEndDate(Date intervalEndDate) {
		this.intervalEndDate = intervalEndDate;
	}
	public Date getIntervalEndTime() {
		return intervalEndTime;
	}
	public void setIntervalEndTime(Date intervalEndTime) {
		this.intervalEndTime = intervalEndTime;
	}
	public Date getIntervalStartDate() {
		return intervalStartDate;
	}
	public void setIntervalStartDate(Date intervalStartDate) {
		this.intervalStartDate = intervalStartDate;
	}
	public Date getIntervalStartTime() {
		return intervalStartTime;
	}
	public void setIntervalStartTime(Date intervalStartTime) {
		this.intervalStartTime = intervalStartTime;
	}
	public int getSeedMinutes() {
		return seedMinutes;
	}
	public void setSeedMinutes(int seedMinutes) {
		this.seedMinutes = seedMinutes;
	}
	public Date getZoneEndDate() {
		return zoneEndDate;
	}
	public void setZoneEndDate(Date zoneEndDate) {
		this.zoneEndDate = zoneEndDate;
	}
	public Date getZoneStartDate() {
		return zoneStartDate;
	}
	public void setZoneStartDate(Date zoneStartDate) {
		this.zoneStartDate = zoneStartDate;
	}
	
	public String getProtectedPremiumHourTypes() {
		return protectedPremiumHourTypes;
	}
	public void setProtectedPremiumHourTypes(String protectedEligiblePremiumHourTypes) {
		this.protectedPremiumHourTypes = protectedEligiblePremiumHourTypes;
	}
	public boolean isAllocateByTeam() {
		return allocateByTeam;
	}
	public void setAllocateByTeam(boolean allocateByTeam) {
		this.allocateByTeam = allocateByTeam;
	}
	public String getHourTypeForWorkDetailsWithOvertime() {
		return hourTypeForWorkDetailsWithOvertime;
	}
	public void setHourTypeForWorkDetailsWithOvertime(
			String hourTypeForWorkDetailsWithOvertime) {
		this.hourTypeForWorkDetailsWithOvertime = hourTypeForWorkDetailsWithOvertime;
	}
	public boolean isSplittingEnabled() {
		return splittingEnabled;
	}
	public void setSplittingEnabled(boolean splittingEnabled) {
		this.splittingEnabled = splittingEnabled;
	}
	public boolean isUpdateWorkdetailDateForThreshold() {
		return updateWorkdetailDateForThreshold;
	}
	public void setUpdateWorkdetailDateForThreshold(
			boolean updateWorkdetailDateForThreshold) {
		this.updateWorkdetailDateForThreshold = updateWorkdetailDateForThreshold;
	}
	public boolean isFullySeeded() {
		return fullySeeded;
	}
	public void setFullySeeded(boolean fullySeeded) {
		this.fullySeeded = fullySeeded;
	}
	public List<EmployeeSchedDtlData> getScheduleDetailsList() {
		return scheduleDetailsList;
	}
	public void setScheduleDetailsList(List<EmployeeSchedDtlData> schedulePeriodsList) {
		this.scheduleDetailsList = schedulePeriodsList;
	}
	public WorkDetailList<WorkDetailData> getRuleCreatedPremiums() {
		
		if(this.ruleCreatedPremiums == null){
			this.ruleCreatedPremiums = new WorkDetailList<WorkDetailData>();
		}
		return ruleCreatedPremiums;
	}
	public void setRuleCreatedPremiums(WorkDetailList<WorkDetailData> ruleCreatedPremiums) {
		this.ruleCreatedPremiums = ruleCreatedPremiums;
	}
	
	public void addPremiumToList(WorkDetailData wd){
		if(this.ruleCreatedPremiums == null){
			this.ruleCreatedPremiums = new WorkDetailList<WorkDetailData>();
		}
		
		this.ruleCreatedPremiums.add(wd);
	}
	
	public void addAllPremiumsToList(List<WorkDetailData> premiums){
		if(this.ruleCreatedPremiums == null){
			this.ruleCreatedPremiums = new WorkDetailList<WorkDetailData>();
		}
		
		this.ruleCreatedPremiums.addAll(premiums);
	}	

	public boolean isApplyFirstHourTypeInHourSetToDetail() {
		return applyFirstHourTypeInHourSetToDetail;
	}
	public void setApplyFirstHourTypeInHourSetToDetail(
			boolean applyFirstHourTypeInHourSetToDetail) {
		this.applyFirstHourTypeInHourSetToDetail = applyFirstHourTypeInHourSetToDetail;
	}
	public boolean isApplyFirstHourTypeInHourSetToPremium() {
		return applyFirstHourTypeInHourSetToPremium;
	}
	public void setApplyFirstHourTypeInHourSetToPremium(
			boolean applyFirstHourTypeInHourSetToPremium) {
		this.applyFirstHourTypeInHourSetToPremium = applyFirstHourTypeInHourSetToPremium;
	}
	
	public Integer getResetTimeCodeDuration(String TimeCodeName) {
		return resetTimeCodeAndDuration.get(TimeCodeName);
	}
	public void setResetTimeCodeAndDuration(String TimeCodeName, Integer duration) {
		resetTimeCodeAndDuration.put(TimeCodeName, duration);
		resetTimeCodeString = null;
	}
	public String getResetTimeCodesInCommaDelimitedString(){
		if(resetTimeCodeString == null){
			String timeCodesStr = "";
			for(Iterator<String> it = resetTimeCodeAndDuration.keySet().iterator(); it.hasNext();){
				timeCodesStr += it.next();
				if(it.hasNext()){
					timeCodesStr += ",";
				}
			}
			
			if(timeCodesStr.length() != 0){
				resetTimeCodeString = timeCodesStr;
			}
		}
		return resetTimeCodeString;
	}
	public boolean isApplyOvertimeToDetails() {
		return applyOvertimeToDetails;
	}
	public void setApplyOvertimeToDetails(boolean applyOvertimeToDetails) {
		this.applyOvertimeToDetails = applyOvertimeToDetails;
	}
	public void setUseFirstHalfOfCycle(boolean useFirstHalfOfCycle) {
		this.useFirstHalfOfCycle = useFirstHalfOfCycle;
	}
	public boolean isAllocationForDaily() {
		return isAllocationForDaily;
	}
	public void setAllocationForDaily(boolean isAllocationForDaily) {
		this.isAllocationForDaily = isAllocationForDaily;
	}
  }
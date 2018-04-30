package com.fcgmd.app.ta.quickrules;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.modules.budgeting.rules.RuleException;
import com.workbrain.app.ta.db.CalcGroupAccess;
import com.workbrain.app.ta.db.EmployeeTeamAccess;
import com.workbrain.app.ta.model.CalcGroupData;
import com.workbrain.app.ta.model.EmployeeSchedDtlData;
import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.EmployeeTeamData;
import com.workbrain.app.ta.model.PayGroupData;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.quickrules.overtime.ConsolidatedOvertimeRuleHelper;
//import com.workbrain.app.ta.quickrules.overtime.ConsolidatedOvertimeRuleParameterData;
import com.workbrain.app.ta.quickrules.overtime.ConsolidatedWeeklyOvertimeValidator;
import com.workbrain.app.ta.quickrules.overtime.HourSetDescriptionProcessor;
import com.workbrain.app.ta.quickrules.validator.RuleValidator;
import com.workbrain.app.ta.ruleengine.Condition;
import com.workbrain.app.ta.ruleengine.ImmutableRuleParameterInfo;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Parm;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleEngineException;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.RuleParameterInfoCache;
import com.workbrain.app.ta.ruleengine.RuleParameterInfoHelper;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.app.ta.util.PayGroupHelper;
import com.workbrain.security.team.WorkbrainTeamTreeManager;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;
import com.workbrain.util.ErrorMessageHelper;
import com.workbrain.util.SimpleTimePeriod;
import com.workbrain.util.StringUtil;
import com.workbrain.util.TimePeriod;
import com.workbrain2.ta.svc.payroll.PayGroupService;

public class FcgmdConsolidatedWeeklyOvertimeRule extends Rule {
    public final static String DAY_WEEK_STARTS_VALUE_DEFAULT = "Default";
    public final String DAY_STARTTIME_DATE_FORMAT = "yyyyMMdd HHmmss";
    public final String DAY_STARTTIME_DATE_DEFAULT = "19000101 000000";
    
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FcgmdConsolidatedWeeklyOvertimeRule.class);

    public final static String APPLY_OVERTIME_TO_DETAIL = "D";
    public final static String APPLY_OVERTIME_TO_PREMIUM = "P";
     
    public final static String PARAM_HOURSET_DESCRIPTION = "HourSetDescription";
    public final static String PARAM_USE_FIRST_HALF_OF_CYCLE = "UseFirstHalfOfCycle";
    public final static String PARAM_APPLY_OVERTIME_TO = "ApplyOvertimeTo";
    public final static String PARAM_APPLY_FIRST_HOUR_TYPE_IN_HOURSET_DETAIL = "ApplyFirstHourTypeInHourSetDetail";
    public final static String PARAM_APPLY_FIRST_HOUR_TYPE_IN_HOURSET_PREMIUM = "ApplyFirstHourTypeInHourSetPremium";
    public final static String PARAM_APPLY_OVERTIME_METHOD_SCHEDULE_FOR_DETAIL = "ApplyOvertimeMethodSchForDetail";
    public final static String PARAM_APPLY_OVERTIME_METHOD_SCHEDULE_FOR_PREMIUM = "ApplyOvertimeMethodSchForPremium";
    
    public final static String PARAM_ELIGIBLE_WORKDETAIL_TIMECODES = "EligibleWorkDetailTimeCodes";
    public final static String PARAM_PROTECTED_WORKDETAIL_TIMECODES = "ProtectedWorkDetailTimeCodes";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "WorkDetailHourTypes";
    
    public final static String PARAM_ELIGIBLE_OVERTIME_PERIOD = "EligibleOvertimePeriod";
    public final static String PARAM_DAY_WEEK_STARTS = "DayWeekStarts";
    
    public final static String PARAM_PROTECTED_ELIGIBLE_HOURTYPES = "ProtectedHourtypes";
    public final static String PARAM_PROTECTED_PREMIUM_TIMECODES = "ProtectedPremiumTimeCodes";
    public final static String PARAM_PROTECTED_PREMIUM_HOURTYPES = "ProtectedPremiumHourTypes";
    public final static String PARAM_ASSIGN_BETTER_HOURTYPE = "AssignBetterHourType";

    public final static String PARAM_PREMIUM_TIMECODE_FOR_OVERTIME = "PremiumTimeCodeForOvertime";
    public final static String PARAM_HOURTYPE_FOR_WORKDETAILS_WITH_OVERTIME = "HourtypeForWorkDetailsWithOvertime";
    public final static String PARAM_POPULATE_PREMIUM_TIME_FIELDS = "PopulatePremiumTimeFields";
    
    public final static String PARAM_ALLOCATE_BY_JOB = "AllocateByJob";
    public final static String PARAM_ALLOCATE_BY_DEPARTMENT = "AllocateByDepartment";
    public final static String PARAM_ALLOCATE_BY_PROJECT = "AllocateByProject";
    public final static String PARAM_ALLOCATE_BY_DOCKET = "AllocateByDocket";
    public final static String PARAM_ALLOCATE_BY_TEAM = "AllocateByTeam";
    public final static String PARAM_ALLOCATE_BY_TIMECODE = "AllocateByTimeCode";
    public final static String PARAM_ALLOCATE_BY_HOURTYPE = "AllocateByHourType";
    
    public final static String PARAM_PREMIUM_JOB       = "PremiumJob";
    public final static String PARAM_PREMIUM_DEPARTMENT = "PremiumDepartment";
    public final static String PARAM_PREMIUM_PROJECT    = "PremiumProject";
    public final static String PARAM_PREMIUM_DOCKET    = "PremiumDocket";
    public final static String PARAM_PREMIUM_QUANTITY   = "PremiumQuantity";
    
    public final static String PARAM_ALLOCATE_TO_NON_HOME_TEAM = "AllocateToNonHomeTeam";
    public final static String PARAM_USE_LOOSE_HOME_TEAM_LOGIC = "UseLooseHomeTeamLogic";
    
    public final static String APPLY_ON_FLSA_PERIOD = "FLSA Period";
    
	public static final String OP_STAR_VALUE = "*";
    
    /**************************
     * DONOT PUT INSTANCE VARIABLES FOR QUICKRULES/RULES
     * ALL CLASS VARIABLES SHOULD BE STATIC and FINAL 
     **************************/    
    private static final List<ImmutableRuleParameterInfo> PARAMETER_INFO;
    

       
    static {      
        List<ImmutableRuleParameterInfo> paramList = new ArrayList<ImmutableRuleParameterInfo>();
        paramList.add(new ImmutableRuleParameterInfo(PARAM_HOURSET_DESCRIPTION, RuleParameterInfo.STRING_TYPE, false));
        
        paramList.add(new ImmutableRuleParameterInfo(PARAM_ELIGIBLE_WORKDETAIL_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PROTECTED_WORKDETAIL_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, false));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PROTECTED_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PROTECTED_PREMIUM_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PROTECTED_PREMIUM_HOURTYPES, RuleParameterInfo.STRING_TYPE, true));
        
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_USE_FIRST_HALF_OF_CYCLE, true));
        
        List<String> otPeriodChoice = new ArrayList<String>();
        otPeriodChoice.add(DateHelper.APPLY_ON_UNIT_WEEK);
        otPeriodChoice.add(DateHelper.APPLY_ON_UNIT_PAYPERIOD);
        otPeriodChoice.add(DateHelper.APPLY_ON_UNIT_MONTH);
        otPeriodChoice.add(APPLY_ON_FLSA_PERIOD);
        paramList.add(new ImmutableRuleParameterInfo(PARAM_ELIGIBLE_OVERTIME_PERIOD, RuleParameterInfo.CHOICE_TYPE, true, otPeriodChoice,
        		DateHelper.APPLY_ON_UNIT_WEEK));
        
        List<String> dayWeekStartChoices = new ArrayList<String>();
        dayWeekStartChoices.add(DAY_WEEK_STARTS_VALUE_DEFAULT);
        dayWeekStartChoices.add("Sunday");
        dayWeekStartChoices.add("Monday");
        dayWeekStartChoices.add("Tuesday");
        dayWeekStartChoices.add("Wednesday");
        dayWeekStartChoices.add("Thursday");
        dayWeekStartChoices.add("Friday");
        dayWeekStartChoices.add("Saturday");
        paramList.add(new ImmutableRuleParameterInfo(PARAM_DAY_WEEK_STARTS, RuleParameterInfo.CHOICE_TYPE, true, dayWeekStartChoices,
                                                     DAY_WEEK_STARTS_VALUE_DEFAULT));
    
        paramList.add(new ImmutableRuleParameterInfo(PARAM_APPLY_OVERTIME_TO, RuleParameterInfo.STRING_TYPE, true));        
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_APPLY_FIRST_HOUR_TYPE_IN_HOURSET_DETAIL, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_APPLY_FIRST_HOUR_TYPE_IN_HOURSET_PREMIUM, true));
        
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_APPLY_OVERTIME_METHOD_SCHEDULE_FOR_DETAIL, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_APPLY_OVERTIME_METHOD_SCHEDULE_FOR_PREMIUM, true));
        
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ASSIGN_BETTER_HOURTYPE, true));
        
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PREMIUM_TIMECODE_FOR_OVERTIME, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_HOURTYPE_FOR_WORKDETAILS_WITH_OVERTIME, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_POPULATE_PREMIUM_TIME_FIELDS, true));
        
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_BY_JOB, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_BY_DEPARTMENT, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_BY_PROJECT, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_BY_DOCKET, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_BY_TEAM, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_BY_TIMECODE, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_BY_HOURTYPE, true));
        
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PREMIUM_JOB, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PREMIUM_DEPARTMENT, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PREMIUM_PROJECT, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PREMIUM_DOCKET, RuleParameterInfo.STRING_TYPE, true));
        paramList.add(new ImmutableRuleParameterInfo(PARAM_PREMIUM_QUANTITY, RuleParameterInfo.STRING_TYPE, true));
        
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_ALLOCATE_TO_NON_HOME_TEAM, true));
        paramList.add(RuleParameterInfoHelper.createImmutableTrueFalseChoiceParam(PARAM_USE_LOOSE_HOME_TEAM_LOGIC, true));
        
        PARAMETER_INFO = Collections.unmodifiableList(paramList);
        RuleParameterInfoCache cache = RuleParameterInfoCache.getInstance();
        cache.addUnmodifiableRuleParameterInfoList(FcgmdConsolidatedWeeklyOvertimeRule.class.getName(), PARAMETER_INFO);        
    }
    
    
    public List<RuleParameterInfo> getParameterInfo (DBConnection conn) {
       return cloneParameterInfoList(PARAMETER_INFO);       
    }
    
    public RuleValidator getValidator(){
      return new ConsolidatedWeeklyOvertimeValidator();
    }    

    /**
    * Weekly Overtime Plus rule entry point.
    */
    public void execute(WBData wbData, Parameters parameters) throws Exception {
        // Retrieve parameters and preprocess them
        FcgmdConsolidatedOvertimeRuleParameterData parameterData = processAndInitializeRuleParameters(wbData, parameters);
        boolean useFirstHalf = Boolean.valueOf(parameters.getParameter(PARAM_USE_FIRST_HALF_OF_CYCLE ,"true")).booleanValue();
    	boolean inFirstHalf = false;
    	boolean isFSLAPeriod = false;
    	boolean invalidCalcUdfs = false;
    	
    	int periodLength = 0;

        if (useFirstHalf && (parameters.getParameter(PARAM_ELIGIBLE_OVERTIME_PERIOD, DateHelper.APPLY_ON_UNIT_WEEK).toUpperCase()).equals("FLSA PERIOD") ){
        	CalcGroupAccess cga = new CalcGroupAccess(wbData.getDBconnection());
        
        	CalcGroupData cgd = cga.load(wbData.getCalcgrpId());
    	
        	Date firstPayPeriod = null;
        	Date today = null;
    		int daysDiff = 0;
    	
        	try{
        		firstPayPeriod = DateHelper.convertStringToDate(cgd.getCalcgrpUdf1(), "MM/dd/yyyy");
        		today = wbData.getWrksWorkDate();
        		daysDiff = DateHelper.getDifferenceInDays(today, firstPayPeriod);
        		periodLength = Integer.parseInt(cgd.getCalcgrpUdf2())/2;
        	}catch(Exception e){
        		throw new RuleException("Please ensure to populate CALC_UDF1 and CALC_UDF2.",e);
        	}
                
        	if ( (daysDiff % (periodLength*2)) < periodLength){
        		inFirstHalf = true;
        	}
        
        	if ( (parameters.getParameter(PARAM_ELIGIBLE_OVERTIME_PERIOD, DateHelper.APPLY_ON_UNIT_WEEK).toUpperCase()).equals("FLSA PERIOD")){
        		isFSLAPeriod = true;
        	}
        
        	if (null == firstPayPeriod || periodLength == 0){
        		invalidCalcUdfs = true;
        	}
        }

        if((useFirstHalf  				// If we are to use the first half.
        		&& inFirstHalf 			// And in the the first half of the period. 
        		&& isFSLAPeriod 		// And is an FSLA type period.
        		&& !invalidCalcUdfs)  	// Ensures that calc UDFs are entered correctly.
        		|| (!useFirstHalf) ){	// or if we are not using the first half
        
        	// determine overtime period
        	Date rulePeriodStartDate = calculateRangeDate(wbData, parameterData, DateHelper.APPLY_ON_FIRST_DAY, null); // for first day
        	Date rulePeriodEndDate   = calculateRangeDate(wbData, parameterData, DateHelper.APPLY_ON_LAST_DAY, null);  // for last day
        
        	//overtime allocation method
        	boolean applyBasedOnSchedule = parameterData.isOvertimeAllocationScheduleBased();
        	boolean allocateToNonHomeTeam = parameterData.isOvertimeAllocationHomeTeamBased();
        	boolean useLooseHomeTeamLogic = parameterData.isUseLooseHomeTeamLogic();
          
        	if(useFirstHalf) {
        		rulePeriodEndDate = DateHelper.addDays(rulePeriodStartDate, (periodLength-1) );
        	}
        	
        	if (applyBasedOnSchedule ) {
            	// apply schedule based overtime
            	ruleWeeklyOvertimeScheduleBased(wbData, parameterData, rulePeriodStartDate, rulePeriodEndDate);
        	} else if(allocateToNonHomeTeam) {
        		//apply overtime based on Home Team: i.e apply OT first to the tempTeam(Borrowed Team) and then the remaining to the homeTeam.
        		ruleWeeklyOvertimeHomeTeamBased(wbData, parameterData, rulePeriodStartDate, rulePeriodEndDate,useLooseHomeTeamLogic );
        	}else{
        		ruleWeeklyOvertime(wbData, parameterData, rulePeriodStartDate, rulePeriodEndDate);
        	}
        
        	// Inserting premiums enabled - see if we need to perform allocation algorithm
        	// on the premiums inserted.
        	if(!parameterData.isApplyOvertimeToDetails()){
            	parameterData.setIntervalStartDate(rulePeriodStartDate);
            	parameterData.setIntervalEndDate(rulePeriodEndDate);
            	FcgmdConsolidatedOvertimeRuleHelper.applyPremiumAllocationByLaborMetrics(wbData, parameterData);
          
            	// Get the premiums created by the rule
            	WorkDetailList<WorkDetailData> premiumsList = parameterData.getRuleCreatedPremiums();
            
            	if(premiumsList != null){
                	if(premiumsList.size() > 1){
                	premiumsList = ConsolidatedOvertimeRuleHelper.mergeSimilarPremiums(premiumsList);
                	}
                	wbData.getRuleData().getWorkPremiums().addAll(premiumsList);
            	}
        	}
        }
    }
    
    /**
    * Schedule Based overtime method to assigns overtime based using existing schedule.
    * 
    * @Note: Needs auto recalc or manual recalc of whole duration for accurate results. 
    *        Not recommended for Monthly Period until Monthly recalc period is added to the calcgroup
    */
    protected void ruleWeeklyOvertimeScheduleBased(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData, Date datePeriodStarts, Date datePeriodEnds) throws Exception {
        int protectedMinutes = FcgmdConsolidatedOvertimeRuleHelper.calculateSeedTimeFromProtectedTimecodesHourtypes(wbData, parameterData, datePeriodStarts, datePeriodEnds, null, null );

        if (wbData.getRuleData().getWorkDetailCount() == 0) return;
        
        Date minStartTime = wbData.getRuleData().getWorkDetail(0).getWrkdStartTime();
        Date maxEndTime = wbData.getRuleData().getWorkDetail(wbData.getRuleData().getWorkDetailCount() - 1).getWrkdEndTime();
        
        int numberOfDays = DateHelper.dateDifferenceInDaysDstAware(datePeriodEnds, datePeriodStarts) + 1;
        
        int minutesInsideScheduleRange[] = new int[numberOfDays];    // each days scheduled minutes
        int minutesWorkDetailRange[] = new int[numberOfDays];        // each days work details
        int seedMinutesInShift[] = new int[numberOfDays];            // cumulative seed for a given date
        int count = 0;
        int totalMinutesInsideScheduleRange = 0;
        int totalMinutesWorkDetailRange = 0;
        int todayIndex = 0;
        
        int uptoOffDayWorkDetailMinutes = 0;
        int uptoOffDayScheduleMinutes   = 0;
        
        List<SimpleTimePeriod> todaysSchedPeriods = null;
        
        List<EmployeeSchedDtlData> fullList = new ArrayList<EmployeeSchedDtlData>();
//TESTTEST
        for (Date date = datePeriodStarts; date.compareTo(datePeriodEnds) <= 0;date = DateHelper.addDays(date, 1)) {
            EmployeeScheduleData schdData = wbData.getEmployeeScheduleData(date);
            List<EmployeeSchedDtlData> schdDetails = wbData.getRuleData().getCalcDataCache().getEmployeeScheduleDetails(schdData.getEmpskdId(), wbData.getDBconnection());
            fullList.addAll(schdDetails);
            
            List<SimpleTimePeriod> timePeriods = ConsolidatedOvertimeRuleHelper.getMergedScheduleDetailsAsPeriods(schdDetails);
            
            for(int schdid = 0; schdid < timePeriods.size(); schdid++){
                SimpleTimePeriod schdInterval = timePeriods.get(schdid);
                
                minutesInsideScheduleRange[count] += FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesForRange(
                    wbData, parameterData, date, date,
                    schdInterval.retrieveStartDate(),schdInterval.retrieveEndDate(), true,
                     parameterData.getEligibleHourTypes(), true);
            }
            
            minutesWorkDetailRange[count] = FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesForRange(
                                            wbData, parameterData, date, date, null, null, true,
                                            parameterData.getEligibleHourTypes(), true);
             
            totalMinutesInsideScheduleRange += minutesInsideScheduleRange[count];
            totalMinutesWorkDetailRange += minutesWorkDetailRange[count];

            if (count == 0) {
                seedMinutesInShift[count] = protectedMinutes;
            } else {
                seedMinutesInShift[count] = seedMinutesInShift[count-1] + minutesInsideScheduleRange[count -1]; // - 1
            }

            //figure out the current workday index in the week
            if (date.compareTo(wbData.getRuleData().getWrksWorkDate()) == 0){
                todayIndex = count;
                todaysSchedPeriods = timePeriods;
                uptoOffDayWorkDetailMinutes = totalMinutesWorkDetailRange - minutesWorkDetailRange[count];
                uptoOffDayScheduleMinutes   = totalMinutesInsideScheduleRange - minutesInsideScheduleRange[count];
            }
            
            count++;
        }
        
        // save the full list for later
        parameterData.setScheduleDetailsList(fullList);

        int minutesOutsideOfShiftUsed = 0;
        int seedBefore = protectedMinutes;
        
        //allocate the seed minutes of TODAY's before and after shifts appropriately
        for (int i=0;i<numberOfDays;i++){
            if (i < todayIndex){
                minutesOutsideOfShiftUsed += (minutesWorkDetailRange[i] - minutesInsideScheduleRange[i]);
            } else if (i == todayIndex) {
                seedBefore += (totalMinutesInsideScheduleRange + minutesOutsideOfShiftUsed);
            }
        }
        
        /**
         *  Let us first see if today is an off day. If so then, we need to calculate the
         *  seed differently.
         *  
         *  The seed for this case has to be: Sum of all the work details until that day
         *  and sum of all the work details that are within the scheduled interval
         *  for the rest of the days.
         */  
        if (!wbData.getRuleData().getEmployeeScheduleData().isEmployeeScheduledActual()){
            parameterData.setFullySeeded(true);
            
            parameterData.setSeedMinutes(protectedMinutes + uptoOffDayWorkDetailMinutes + (totalMinutesInsideScheduleRange - uptoOffDayScheduleMinutes));
             
            parameterData.setIntervalStartTime(minStartTime);
            parameterData.setIntervalEndTime(maxEndTime);
            parameterData.setIntervalStartDate(wbData.getRuleData().getWrksWorkDate());
            parameterData.setIntervalEndDate(wbData.getRuleData().getWrksWorkDate());
            parameterData.setZoneStartDate(datePeriodStarts);
            parameterData.setZoneEndDate(datePeriodEnds);
            FcgmdConsolidatedOvertimeRuleHelper.applyOvertimeRule(wbData, parameterData);
            return;
        }

        // for the following cases we are going to provide full seed so that it won't need to be
        // readjusted.
        parameterData.setFullySeeded(true);
        
        int inShiftSeed = 0;
        if (todayIndex == 0) {
            inShiftSeed = protectedMinutes;
        } else {
            inShiftSeed = seedMinutesInShift[todayIndex];
        }
        
        for(int inshiftId = 0; inshiftId < todaysSchedPeriods.size(); inshiftId++){
            // in shift
            SimpleTimePeriod inShiftPeriod = todaysSchedPeriods.get(inshiftId);
            parameterData.setIntervalStartDate(wbData.getRuleData().getWrksWorkDate());
            parameterData.setIntervalEndDate(wbData.getRuleData().getWrksWorkDate());
            parameterData.setIntervalStartTime(inShiftPeriod.retrieveStartDate());
            parameterData.setIntervalEndTime(inShiftPeriod.retrieveEndDate());
            parameterData.setSeedMinutes(inShiftSeed);
            parameterData.setInShiftStartTime(inShiftPeriod.retrieveStartDate());
            parameterData.setInShiftEndTime(inShiftPeriod.retrieveEndDate());
            parameterData.setZoneStartDate(datePeriodStarts);
            parameterData.setZoneEndDate(datePeriodEnds);
        
            FcgmdConsolidatedOvertimeRuleHelper.applyOvertimeRule(wbData, parameterData);
            
            //get ready for the next shift
            inShiftSeed += FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesFromDetails(
                    wbData, parameterData, inShiftPeriod.retrieveStartDate(), inShiftPeriod.retrieveEndDate(), true, parameterData.getEligibleHourTypes(), true);
        }
        
        List<TimePeriod> outShiftPeriods = FcgmdConsolidatedOvertimeRuleHelper.calculateOutsideSchedulePeriods(wbData, todaysSchedPeriods, parameterData);
        int outshiftSeed = seedBefore;
        
        for(int outshiftId = 0; outshiftId < outShiftPeriods.size(); outshiftId++ ){
            // outshifts
            TimePeriod outShiftPeriod = outShiftPeriods.get(outshiftId);
            parameterData.setSeedMinutes(outshiftSeed);
            parameterData.setInShiftStartTime(outShiftPeriod.retrieveStartDate());
            parameterData.setInShiftEndTime(outShiftPeriod.retrieveEndDate());
            parameterData.setZoneStartDate(datePeriodStarts);
            parameterData.setZoneEndDate(datePeriodEnds);
            FcgmdConsolidatedOvertimeRuleHelper.applyOvertimeRule(wbData, parameterData);
    
            outshiftSeed += FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesFromDetails(wbData, parameterData,
                    outShiftPeriod.retrieveStartDate(), outShiftPeriod.retrieveEndDate(), true, parameterData.getEligibleHourTypes(), true);
        }

        // applyRates();
    }
    
    
    /**
     * Team Based overtime method to assign overtime based on the employee home and temp teams.
     * Assign OT first to the Temporary team and then the remaining to the home team 
     * 
     * @Note: Needs auto recalc or manual recalc of whole duration for accurate results. 
     */
    protected void ruleWeeklyOvertimeHomeTeamBased ( WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData, Date datePeriodStarts, Date datePeriodEnds, boolean useLooseHomeTeamLogic ) throws Exception {
    	
    	List<EmployeeTeamData> empTeamData = new ArrayList<EmployeeTeamData>();
    	EmployeeTeamAccess eta = new EmployeeTeamAccess(wbData.getDBconnection());
    	empTeamData =  eta.load( wbData.getEmpId(), datePeriodStarts, datePeriodEnds);
    	Iterator<EmployeeTeamData> it = empTeamData.iterator();
    	EmployeeTeamData etd =  new EmployeeTeamData();
    	List<Long> homeTeams = new ArrayList<Long>();
    	List<Long> tempTeams = new ArrayList<Long>();
    	List<Long> newHomeTeams = new ArrayList<Long>();
    	//Determine the Home and Temporary Teams of the employee.
    	while( it.hasNext() ) {
    		etd = it.next();
    		if(etd.getEmptHomeTeam().equals("Y")  &&  !homeTeams.contains(etd.getWbtId())  ){
    			homeTeams.add( etd.getWbtId() );
    		}else if( !tempTeams.contains( etd.getWbtId() ) ){
    			tempTeams.add( etd.getWbtId() );
    		}
    	}

    	WorkbrainTeamTreeManager wtm = new WorkbrainTeamTreeManager(wbData.getDBconnection());
    	//If employee has more than one home teams, we ignore the "Use home Team Logic parameter ,and continue as normal rule"
    	if(homeTeams.size()>1){
    		ruleWeeklyOvertime(wbData, parameterData, datePeriodStarts, datePeriodEnds);
    	} else { //Else,Apply the Allocate To Non-Home Team Logic

    		//Determine the list of new home teams based on some conditions when the UseLooseHomeTeamLogic is checked.
    		if ( useLooseHomeTeamLogic ) {
    			for( int i=0 ; i < tempTeams.size()   ;i++ ) {
    				for( int j=0; j <  homeTeams.size() ; j++ ) {
    					long tempT=tempTeams.get(i);
    					long homeT=homeTeams.get(j);
    					if( homeT!=0 && tempT!=0 ) {
    						//Case 1: The employee's home team and the non-home team have the same immediate parent team.
    						if ( wtm.getImmediateParentID(tempT) == wtm.getImmediateParentID(homeT) ) {
    							newHomeTeams.add(tempT);
							}
    						//Case 2: The employee's home team IS the parent of the non-home team.
    						else if ( wtm.getImmediateParentID(tempT)==homeT ) {
    							newHomeTeams.add(tempT);
    						}
    						// Case 3:The non-home team is the parent of the employee's home team.
    						else if ( wtm.getImmediateParentID(homeT)==tempT ) {
    							newHomeTeams.add(tempT);
    						}
    					}
    				}
    			}
    		}
    		newHomeTeams.addAll( homeTeams );
    		int numberOfDays = DateHelper.dateDifferenceInDaysDstAware(datePeriodEnds, datePeriodStarts) + 1;

    		int minutesInsidehomeTeam[] = new int[numberOfDays];    // each days scheduled minutes
    		int minutesWorkDetailRange[] = new int[numberOfDays];        // each days work details
    		int seedMinutesInShift[] = new int[numberOfDays];            // cumulative seed for a given date
    		int minutesOutsideHomeTeam[] = new int[numberOfDays];            // cumulative seed for a given date
    		int count = 0;
    		int totalMinutesInsideHomeTeam = 0;
    		int totalMinutesOutsideHomeTeam = 0;
    		int todayIndex = 0;
    		int protectedMinutes = FcgmdConsolidatedOvertimeRuleHelper.calculateSeedTimeFromProtectedTimecodesHourtypes(wbData, parameterData, datePeriodStarts, datePeriodEnds, null, null );
    		int seedBefore = protectedMinutes;
    		List<SimpleTimePeriod> todaysInHomePeriods = null;
    		List<SimpleTimePeriod> todaysTimePeriods = null;
    		WorkDetailList<WorkDetailData> WD_InHomeTeam= new WorkDetailList<WorkDetailData>();
        	WorkDetailList<WorkDetailData> WD_OutsideHomeTeam =  new WorkDetailList<WorkDetailData>();

    		//Determine the work details in Home and non-home teams. Also determine the number of minutes worked in those teams.
    		for ( Date date = datePeriodStarts; date.compareTo(datePeriodEnds) <= 0;date = DateHelper.addDays(date, 1) ) {
    			WorkDetailList<WorkDetailData> wrkDetails = wbData.getWorkDetailsForDate(date);
    			WorkDetailList<WorkDetailData> merged_wrkDetails = ConsolidatedOvertimeRuleHelper.mergeSimilarPremiums(wrkDetails);

    			List<SimpleTimePeriod> WDtimePeriods = ConsolidatedOvertimeRuleHelper.getMergedWrkDetailsAsPeriods(wrkDetails);

    			for( int i = 0; i < merged_wrkDetails.size(); i++ ) {
    				WorkDetailData wd = merged_wrkDetails.get(i);

    				if ( newHomeTeams.contains(wd.getWbtId()) ) {   // WD in Home team
    					minutesInsidehomeTeam[count] += FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesForRange(
    							wbData, parameterData, date, date,
    							wd.retrieveStartDate(),wd.retrieveEndDate(), true,
    							parameterData.getEligibleHourTypes(), true);

    					WD_InHomeTeam.add( wd );
    				}else {											//WD in non-home team						
    					minutesOutsideHomeTeam[count] += FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesForRange(
    							wbData, parameterData, date, date,
    							wd.retrieveStartDate(),wd.retrieveEndDate(), true,
    							parameterData.getEligibleHourTypes(), true);

    					WD_OutsideHomeTeam.add( wd );
    				}
    			}

    			minutesWorkDetailRange[count] = FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesForRange(
    					wbData, parameterData, date, date, null, null, true,
    					parameterData.getEligibleHourTypes(), true);
    			totalMinutesInsideHomeTeam += minutesInsidehomeTeam[count];
    			totalMinutesOutsideHomeTeam += minutesOutsideHomeTeam[count];

    			if (count == 0) {
    				seedMinutesInShift[count] = protectedMinutes;
    			} else {
    				seedMinutesInShift[count] = seedMinutesInShift[count-1] + minutesInsidehomeTeam[count -1]; // - 1
    			}

    			//figure out the current workday index in the week
    			if ( date.compareTo( wbData.getRuleData().getWrksWorkDate()) == 0 ){
    				todayIndex = count;
    				todaysInHomePeriods=FcgmdConsolidatedOvertimeRuleHelper.getTimePeriodsInsideHomeTeam(wbData, WDtimePeriods, parameterData, newHomeTeams);
    				todaysTimePeriods = WDtimePeriods;
    			}
    			count++;
    		}

    		int inShiftSeed = 0;
    		if ( todayIndex == 0 ) {
    			inShiftSeed = protectedMinutes;
    		} else {
    			inShiftSeed = seedMinutesInShift[todayIndex];
    		}
    		inShiftSeed=protectedMinutes+seedMinutesInShift[todayIndex];

    		//If the employee worked in his home team for the entire period, we ignore the Non-Home Team logic.
    		if( totalMinutesOutsideHomeTeam==0 ) {
    			ruleWeeklyOvertime( wbData, parameterData, datePeriodStarts, datePeriodEnds );
    		} else {
    			//allocate the seed minutes of TODAY's before and after shifts appropriately
    			int minutesOutsideOfHomeUsed=0;
    			for ( int i=0;i<numberOfDays;i++ ) {
    				if (i < todayIndex ) {
    					minutesOutsideOfHomeUsed += minutesOutsideHomeTeam[i] ;
    				} else if ( i == todayIndex ) {
    					seedBefore += ( totalMinutesInsideHomeTeam + minutesOutsideOfHomeUsed );//Minutes in Home Team + Minutes in Temp Team before the wrksworkday
    				}
    			}

    			parameterData.setFullySeeded(true);
    			for ( int i=0; i< todaysInHomePeriods.size(); i++ ) {
    				//Calculate for home team first
    				SimpleTimePeriod inHome = todaysInHomePeriods.get(i);
    				parameterData.setIntervalStartDate(wbData.getRuleData().getWrksWorkDate());
    				parameterData.setIntervalEndDate(wbData.getRuleData().getWrksWorkDate());
    				parameterData.setIntervalStartTime(inHome.retrieveStartDate());
    				parameterData.setIntervalEndTime(inHome.retrieveEndDate());
    				parameterData.setSeedMinutes(inShiftSeed);// need to think
    				parameterData.setInShiftStartTime(inHome.retrieveStartDate());
    				parameterData.setInShiftEndTime(inHome.retrieveEndDate());
    				parameterData.setZoneStartDate(datePeriodStarts);
    				parameterData.setZoneEndDate(datePeriodEnds);
    				FcgmdConsolidatedOvertimeRuleHelper.applyOvertimeRule(wbData, parameterData);
    				inShiftSeed += FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesFromDetails(
    						wbData, parameterData, inHome.retrieveStartDate(), inHome.retrieveEndDate(), true, parameterData.getEligibleHourTypes(), true);
    			}
    			List<SimpleTimePeriod> outHomePeriods = FcgmdConsolidatedOvertimeRuleHelper.calculateWDOutsideHomeTeam(wbData, todaysTimePeriods, parameterData, newHomeTeams);
    			int outshiftSeed = seedBefore;

    			for ( int outshiftId = 0; outshiftId < outHomePeriods.size(); outshiftId++ ) {
    				// Calculating for the times worked outside Home Team
    				TimePeriod outShiftPeriod = outHomePeriods.get(outshiftId);
    				parameterData.setSeedMinutes(outshiftSeed);
    				parameterData.setInShiftStartTime(outShiftPeriod.retrieveStartDate());
    				parameterData.setInShiftEndTime(outShiftPeriod.retrieveEndDate());
    				parameterData.setZoneStartDate(datePeriodStarts);
    				parameterData.setZoneEndDate(datePeriodEnds);
    				FcgmdConsolidatedOvertimeRuleHelper.applyOvertimeRule(wbData, parameterData);
    				parameterData.setFullySeeded(true);

    				outshiftSeed += FcgmdConsolidatedOvertimeRuleHelper.calculateMinutesOnlyFromEligibleTimeCodesFromDetails(wbData, parameterData,
    						outShiftPeriod.retrieveStartDate(), outShiftPeriod.retrieveEndDate(), true, parameterData.getEligibleHourTypes(), true);
    			}
    		}
    	}
    }

    /*
    * Applies overtime rule for the case:
    * AS ACCRUES, NO SPLITTING
    * It works for durations: WEEKLY/PAY PERIOD/MONTH
    */
    protected void ruleWeeklyOvertime(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData,
                                      Date datePeriodStarts, Date datePeriodEnds) throws Exception {
        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        
        // Based on the range, calculate the seed from the protected time codes and hour types
        seedMinutes = FcgmdConsolidatedOvertimeRuleHelper.calculateSeedTimeFromProtectedTimecodesHourtypes(wbData, parameterData, datePeriodStarts, datePeriodEnds, null, null);
        
        parameterData.setIntervalStartTime(null);
        parameterData.setIntervalEndTime(null);
        parameterData.setIntervalStartDate(datePeriodStarts);
        parameterData.setIntervalEndDate(workSummaryDate);
        parameterData.setSeedMinutes(seedMinutes);
        parameterData.setZoneStartDate(datePeriodStarts);
        parameterData.setZoneEndDate(datePeriodEnds);
        FcgmdConsolidatedOvertimeRuleHelper.applyOvertimeRule(wbData, parameterData);
    }

    public String getComponentName() {
        return "FCGMD Weekly Overtime Plus Rule";
    }

    public String getComponentUI() {
        return "/quickrules/qFCGMDConsolidatedWeeklyOvertimeParams.jsp";
    }

    public List<Condition> getSuitableConditions() {
        return null;
    }
    
    /**
    * Parses the rule parameters and captures them in a value object.
    * 
    * @param wbData
    * @param parameters input rule parameters
    * @return value object of type ConsolidatedWeeklyRuleParameterData capturing rule parameters
    * @throws Exception
    */
    private FcgmdConsolidatedOvertimeRuleParameterData processAndInitializeRuleParameters(WBData wbData, Parameters parameters) throws Exception{
        boolean applyOvertimeToDetail = true;
        
        FcgmdConsolidatedOvertimeRuleParameterData parameterData = new FcgmdConsolidatedOvertimeRuleParameterData();
        //Determine overtime application mode - detail or premium
        String applyOTTarget = parameters.getParameter(PARAM_APPLY_OVERTIME_TO, APPLY_OVERTIME_TO_DETAIL);
        if(APPLY_OVERTIME_TO_PREMIUM.equalsIgnoreCase(applyOTTarget)) {
            applyOvertimeToDetail = false;
        }
        parameterData.setApplyOvertimeToDetails(applyOvertimeToDetail);
                
        //process common parameters
        parameterData.setHourSetDescription(
                (preprocessHourSetDescription(wbData,
                        parameters.getParameter(PARAM_HOURSET_DESCRIPTION))).toUpperCase());

        //First half of Cycle Param
        parameterData.setUseFirstHalfOfCycle( Boolean.valueOf(parameters.getParameter(PARAM_USE_FIRST_HALF_OF_CYCLE, "true")).booleanValue());
        
        // hour types and time codes - preprocess and set them
        insertNormalizedTimeCodesHourTypes(parameterData, parameters, applyOvertimeToDetail);
        
        parameterData.setOvertimePeriod(parameters.getParameter(PARAM_ELIGIBLE_OVERTIME_PERIOD, DateHelper.APPLY_ON_UNIT_WEEK));
        parameterData.setDayWeekStarts(parameters.getParameter(PARAM_DAY_WEEK_STARTS, DAY_WEEK_STARTS_VALUE_DEFAULT));
        
        if (applyOvertimeToDetail) {
            parameterData.setOvertimeAllocationScheduleBased(Boolean.valueOf(parameters.getParameter(PARAM_APPLY_OVERTIME_METHOD_SCHEDULE_FOR_DETAIL, "false")).booleanValue());
            parameterData.setAssignBetterHourType(Boolean.valueOf(parameters.getParameter(PARAM_ASSIGN_BETTER_HOURTYPE, "false")).booleanValue()); 
            parameterData.setApplyFirstHourTypeInHourSetToDetail(Boolean.valueOf(parameters.getParameter(PARAM_APPLY_FIRST_HOUR_TYPE_IN_HOURSET_DETAIL, "true")).booleanValue());
            parameterData.setOvertimeAllocationHomeTeamBased(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_TO_NON_HOME_TEAM, "false")).booleanValue());
            parameterData.setUseLooseHomeTeamLogic(Boolean.valueOf(parameters.getParameter(PARAM_USE_LOOSE_HOME_TEAM_LOGIC, "false")).booleanValue());
        } else {
            parameterData.setApplyFirstHourTypeInHourSetToPremium(Boolean.valueOf(parameters.getParameter(PARAM_APPLY_FIRST_HOUR_TYPE_IN_HOURSET_PREMIUM, "false")).booleanValue());
            parameterData.setPremiumTimeCodeForOvertime(parameters.getParameter(PARAM_PREMIUM_TIMECODE_FOR_OVERTIME, null));
            parameterData.setAllocateByTimeCode(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_TIMECODE, "false")).booleanValue());
            parameterData.setPopulatePremiumTimeFields(Boolean.valueOf(parameters.getParameter(PARAM_POPULATE_PREMIUM_TIME_FIELDS, "false")).booleanValue());
            parameterData.setOvertimeAllocationScheduleBased(Boolean.valueOf(parameters.getParameter(PARAM_APPLY_OVERTIME_METHOD_SCHEDULE_FOR_PREMIUM, "false")).booleanValue());
            
            parameterData.setAllocateByJob(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_JOB, "false")).booleanValue());
            parameterData.setAllocateByDepartment(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_DEPARTMENT, "false")).booleanValue());
            parameterData.setAllocateByProject(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_PROJECT, "false")).booleanValue());
            parameterData.setAllocateByDocket(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_DOCKET, "false")).booleanValue());
            parameterData.setAllocateByTeam(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_TEAM, "false")).booleanValue());
            parameterData.setAllocateByTimeCode(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_TIMECODE, "false")).booleanValue());
            parameterData.setAllocateByHourType(Boolean.valueOf(parameters.getParameter(PARAM_ALLOCATE_BY_HOURTYPE, "false")).booleanValue());
            //Only consider marker hour type if not allocating by hour type so overtime will not be allocated by marker hour type
            if (!parameterData.isAllocateByHourType()) {
                parameterData.setHourTypeForWorkDetailsWithOvertime(parameters.getParameter(PARAM_HOURTYPE_FOR_WORKDETAILS_WITH_OVERTIME, null));
            }
            
            parameterData.setPremiumJob(parameters.getParameter(PARAM_PREMIUM_JOB, null));
            parameterData.setPremiumDepartment(parameters.getParameter(PARAM_PREMIUM_DEPARTMENT, null));
            parameterData.setPremiumProject(parameters.getParameter(PARAM_PREMIUM_PROJECT, null));
            parameterData.setPremiumDocket(parameters.getParameter(PARAM_PREMIUM_DOCKET, null));
            parameterData.setPremiumQuantity(parameters.getParameter(PARAM_PREMIUM_QUANTITY, null));                
            
            if (parameterData.isAllocateByDepartment() || parameterData.isAllocateByJob() || parameterData.isAllocateByProject() || parameterData.isAllocateByDocket() 
               || parameterData.isAllocateByTeam() || parameterData.isAllocateByTimeCode() || parameterData.isAllocateByHourType()) { 
                parameterData.setPopulatePremiumTimeFields(false);
                parameterData.setOvertimeAllocationScheduleBased(false);
            }            
            // For premiums case, protected premium time codes and hour types should be empty
        }

        // After this everyone should read from this instance.
        return parameterData;
    }
    
    protected RuleParameterInfo createRuleParamChoiceType(String name, boolean isOptional){
        RuleParameterInfo parameterInfo = new RuleParameterInfo(name, RuleParameterInfo.CHOICE_TYPE, isOptional);
        parameterInfo.addChoice("true");
        parameterInfo.addChoice("false");
        return parameterInfo;
    }
    
    /**
    * It is possible for the eligible time codes and hour types to have same values that are in
    * protected time codes and hour types. When that happens the following method removes those
    * duplicates from the eligible time codes and hour types. Is behaviour is as per the daily
    * overtime specification.
    * 
    * @param parameterData
    * @param parameters
    */
    private void insertNormalizedTimeCodesHourTypes(FcgmdConsolidatedOvertimeRuleParameterData parameterData,
                                                    Parameters parameters, boolean applyOvertimeToDetail){        
        // Extract time code strings
        String eligibleTimeCodes = parameters.getParameter(PARAM_ELIGIBLE_WORKDETAIL_TIMECODES,null);
        String protectedTimeCodes = parameters.getParameter(PARAM_PROTECTED_WORKDETAIL_TIMECODES, null);
        String protectedPremiumTimeCodes = parameters.getParameter(PARAM_PROTECTED_PREMIUM_TIMECODES, null);
        
        // Extract hour type strings
        String eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES,"REG");
        String protectedHourTypes = parameters.getParameter(PARAM_PROTECTED_ELIGIBLE_HOURTYPES,null);
        String protectedPremiumHourTypes = parameters.getParameter(PARAM_PROTECTED_PREMIUM_HOURTYPES, null);
        
        if(applyOvertimeToDetail){
            //If protected time codes or hour types are present then some eligible hour types/time codes may be protected.
            //So we will have to consider the non-present protected parameter the same as its eligible counter part.
            //This is done to ensure all combinations of protected time code with hour type are accounted for when
            //calculating overtime minutes.
            if(protectedTimeCodes != null || protectedHourTypes != null) {
                //If no protected time codes specified then all eligible time codes are protected
                if(protectedTimeCodes == null){
                    protectedTimeCodes = eligibleTimeCodes;
                }
                //If no protected hour types specified then all eligible hour types are protected
                if(protectedHourTypes == null) {
                    protectedHourTypes = eligibleHourTypes;
                }
            }
            parameterData.setProtectedWorkDetailTimeCodes(protectedTimeCodes);
            parameterData.setProtectedHourTypes(protectedHourTypes);
        }
        
        // insert normalized time code strings
        parameterData.setEligibleWorkDetailTimeCodes(eligibleTimeCodes);
        parameterData.setProtectedPremiumTimeCodes(protectedPremiumTimeCodes);
        
        // insert normalized hour type strings
        parameterData.setEligibleHourTypes(eligibleHourTypes);
        parameterData.setProtectedPremiumHourTypes(protectedPremiumHourTypes);
    }
    
    /**
    * Calculates the range date based on the overtime period and whether we want
    * the first day of the period or last day of the period.
    */
    private Date calculateRangeDate(WBData wbData, FcgmdConsolidatedOvertimeRuleParameterData parameterData, String applyOnValue, Date basedOnDate) throws Exception {
        Date rangeDate = null;
        if(basedOnDate == null) {
            basedOnDate = wbData.getWrksWorkDate();
        }
        String applyOnUnit = parameterData.getOvertimePeriod();
        
        if(DateHelper.APPLY_ON_UNIT_MONTH.equalsIgnoreCase(applyOnUnit)) {
            rangeDate = DateHelper.getUnitMonth(applyOnValue , false, basedOnDate);
        } else if (DateHelper.APPLY_ON_UNIT_PAYPERIOD.equalsIgnoreCase(applyOnUnit)) {
            PayGroupData pgd = wbData.getRuleData().getCodeMapper().getPayGroupById(wbData.getPaygrpId());
            rangeDate = PayGroupHelper.getUnitPayPeriod(applyOnValue , false, basedOnDate , pgd);
        } else if (APPLY_ON_FLSA_PERIOD.equalsIgnoreCase(applyOnUnit) || applyOnUnit.equalsIgnoreCase("FLSA")){ 
        	CalcGroupData cgd = wbData.getRuleData().getCodeMapper().getCalcGroupById(wbData.getCalcgrpId());
        	rangeDate = getFlsaPeriod(applyOnValue, false, basedOnDate, cgd);
        } else {// the default is the week
            String dayWeekStarts = parameterData.getDayWeekStarts();
            if (dayWeekStarts.equalsIgnoreCase(DAY_WEEK_STARTS_VALUE_DEFAULT)) {
                //getUnitWeek use the DAY_WEEK_STARTS registry parameter to get the start of the week
                rangeDate = DateHelper.getUnitWeek(applyOnValue, false, basedOnDate);
            } else {
                rangeDate = DateHelper.getWeeksFirstDate(basedOnDate, dayWeekStarts);
                if (applyOnValue.equalsIgnoreCase(DateHelper.APPLY_ON_LAST_DAY)) {
                    rangeDate = DateHelper.addDays(rangeDate , 6);
                }
            }
        }        
        return rangeDate;
    }

	private Date getFlsaPeriod(String applyOnValue, boolean b,
			Date dateIn, CalcGroupData cgd) throws RuleException {
		
        Date startOfPeriod = null;
        try {
        	startOfPeriod = DateHelper.parseDate(cgd.getCalcgrpUdf1(), "MM/dd/yyyy");
        } catch (Exception e){
        	throw new RuleException("FLSA start of period value unparsable -- use MM/dd/yyyy format",e);
        }
        
        int lengthOfPeriod = 0;
        try {
        	lengthOfPeriod = Integer.valueOf(cgd.getCalcgrpUdf2());
            if (lengthOfPeriod<= 0) throw new Exception();
        } catch (Exception e){
        	throw new RuleException("FLSA period length is unparsable less than 1",e);
        }
        
        int daysSinceInception = DateHelper.getDifferenceInDays(dateIn, startOfPeriod);
        if (daysSinceInception<0) throw new RuleException("Inception Date must be in the past");

        int daysIntoPeriod = daysSinceInception % lengthOfPeriod;
        // this will always be a positive integer, between 0 and length of period.

        Date date = null;
        startOfPeriod = DateHelper.addDays(dateIn, (-1)*daysIntoPeriod);
        
        if( applyOnValue.equals( DateHelper.APPLY_ON_FIRST_DAY ) ) {
            date = startOfPeriod;
        }
        else if( applyOnValue.equals( DateHelper.APPLY_ON_LAST_DAY ) ) {
            date = DateHelper.addDays(startOfPeriod,lengthOfPeriod-1);
        } 
        return new Datetime( date );
	}
	
	
	 /**
	  * Calculates and replaces all the tokens with actual minute values.
	  * 
	  * @param wbData
	  * @param inputHourSetDesc
	  * @return
	  * @throws Exception
	  */
	 public static String preprocessHourSetDescription(WBData wbData, String inputHourSetDesc) throws Exception{
		 String hourSetResult = null;
		 inputHourSetDesc = inputHourSetDesc.replaceAll("[\\n\\r]", "");

		 Parameters parsedHourSet = Parameters.parseParameters(inputHourSetDesc.toUpperCase());
		 Enumeration<Parm> enumHourSetValues = parsedHourSet.getParameters();
		 
		 int hourIndex = 0;
		 while(enumHourSetValues.hasMoreElements()){
			 String hourTypeName = (enumHourSetValues.nextElement())._name;
			 boolean starOperator = false;
			 String hourTypeValueStr = parsedHourSet.getParameter(hourTypeName);
			 
			 if(hourTypeValueStr.endsWith(OP_STAR_VALUE)) {
				 starOperator = true;
				 hourTypeValueStr = hourTypeValueStr.substring(0, hourTypeValueStr.length()-1);
			 }
			 
			 int minutesForHourType = calculateMinutes(wbData,hourTypeName, hourTypeValueStr);
			 
			 if(hourIndex == 0){
				 hourSetResult = hourTypeName + "=" + Integer.toString(minutesForHourType);
			 }else if(minutesForHourType != 0){
				 // skip the zero minutes stuff if it is not the first hour type in the list
				 hourSetResult += "," + hourTypeName + "=" + Integer.toString(minutesForHourType);
			 }
			 
			 if(starOperator)
			 {
				 hourSetResult += OP_STAR_VALUE;
			 }
			 hourIndex++;
		 }
		 
		 return hourSetResult;
	 }
	 
	 /**
	  * Obtains the minute value for the hour type
	  * 
	  * @param wbData
	  * @param hourDesc
	  * @return
	  * @throws Exception
	  */
	 private static int calculateMinutes(WBData wbData, String hourTypeName, String hourDesc) throws Exception{
		 
			List<String> eligibleEmpVals = new ArrayList<String>();
			
			for(int i = 1; i <= 20; i++) {
					eligibleEmpVals.add(HourSetDescriptionProcessor.EMP_VAL_PREFIX + i);
		    }
			
			
		 double doubleMinutes = 0;
		 String  minStr = hourDesc;
		 boolean multExist = false;
		 double multValue = 0.0;
		 boolean caseOfEmpval = true;
		 String  empValOrUdfName = null;
		 
		 hourTypeName = hourTypeName.trim();
		 hourDesc = hourDesc.trim();
		 if(hourDesc.startsWith("EMP")){
				String deftStr = null;
				String empPrefixStr = hourDesc;
				
				int idxStart = hourDesc.indexOf(HourSetDescriptionProcessor.OP_DEFT_VALUE);
				
				if(idxStart > 0){
					deftStr = hourDesc.substring(idxStart + 1);
					empPrefixStr = hourDesc.substring(0, idxStart);
				}
				
				idxStart = empPrefixStr.indexOf(HourSetDescriptionProcessor.OP_MULT_START);
				int idxEnd   = empPrefixStr.indexOf(HourSetDescriptionProcessor.OP_MULT_END);
				
				// indexes have to be strictly greater than zero
				if((idxStart > 0) && (idxEnd > 0) && (idxStart < idxEnd)){
					String multString = hourDesc.substring(idxStart + 1, idxEnd);
					multValue = Double.parseDouble(StringUtil.trimAll(multString));
					empPrefixStr = hourDesc.substring(0, idxStart);
					multExist = true;
				}
				
				//*** check i.e REG=EMP_VAL1(60)|2400
				 if (hourDesc.startsWith(HourSetDescriptionProcessor.EMP_VAL_PREFIX)) {
					 empPrefixStr = empPrefixStr.trim();
					 empValOrUdfName = empPrefixStr;
					 if (!eligibleEmpVals.contains(empPrefixStr)) {
						 // validation problem
						 throw new RuleEngineException (localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_EMPLOYEE_VALUE_UNDEFINED, HourSetDescriptionProcessor.HOURSET_EMPLOYEE_VALUE_UNDEFINED_default, empPrefixStr));
					 }
					 minStr = (String)wbData.getRuleData().getEmployeeData().getField(empPrefixStr);
					 
					 // Check to see if the value in EMPVAL is -ve
					 if(!StringUtil.isEmpty(minStr) ){
						 try{
							 doubleMinutes = Double.parseDouble(StringUtil.trimAll(minStr));
							 if(doubleMinutes < 0){
								 throw new RuleEngineException (localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_INVALID_EMPVAL, HourSetDescriptionProcessor.HOURSET_INVALID_EMPVAL_default, empPrefixStr+  " = " + minStr));
							 }
						 }catch (NumberFormatException ex) {
							 throw new RuleEngineException (localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_INVALID_EMPVAL, HourSetDescriptionProcessor.HOURSET_INVALID_EMPVAL_default, empPrefixStr+  " = " + minStr));
					 	 }
					 }
					 if (logger.isDebugEnabled()) logger.debug("Resolved " +empPrefixStr+" to :" + minStr);
				 }
				 // *** check i.eREG=EMP_UDF~XX(60)|2400
				 else if (hourDesc.startsWith(HourSetDescriptionProcessor.EMP_UDF_PREFIX)) {
					 caseOfEmpval = false;
					 int empudfIndex = empPrefixStr.indexOf("~");
					 if (empudfIndex == -1) {
						 // validation problem
						 throw new RuleEngineException (localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_EMPLOYEE_UDF_UNDEFINED, HourSetDescriptionProcessor.HOURSET_EMPLOYEE_UDF_UNDEFINED_default));
					 }
					 String empUdfName = empPrefixStr.substring(empudfIndex + 1);
					 empUdfName = empUdfName.trim();
					 empValOrUdfName = empUdfName;
					 try{
						 minStr = wbData.getEmpUdfValue(empUdfName);
						 
						 //	Check to see if the value in EMPUDF is -ve
						 if(!StringUtil.isEmpty(minStr)){
							 try{
								 doubleMinutes = Double.parseDouble(StringUtil.trimAll(minStr));
								 if(doubleMinutes < 0){
									 throw new RuleEngineException (localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_INVALID_EMPUDF, HourSetDescriptionProcessor.HOURSET_INVALID_EMPUDF_default, empUdfName+  " = " + minStr));
								 }
							 }catch (NumberFormatException ex){
								 throw new RuleEngineException (localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_INVALID_EMPUDF, HourSetDescriptionProcessor.HOURSET_INVALID_EMPUDF_default, empUdfName+  " = " + minStr));
						 	 }
				 		 }
						 if (logger.isDebugEnabled()) logger.debug("Resolved " + empPrefixStr +" to :" + minStr);
					 }catch(RuntimeException e){
						 if(StringUtil.isEmpty(deftStr)){
							 throw e; // rethrow the exception
						 }else{
							 // id default value is present, we are fine
							 minStr = null;
						 }
					 }
				 }
				 
				 if(StringUtil.isEmpty(minStr) && !StringUtil.isEmpty(deftStr)) {
					 minStr = deftStr;
					 multExist = false; // no need to apply multiplication
				 } else if(StringUtil.isEmpty(minStr) && StringUtil.isEmpty(deftStr)){
					 if(caseOfEmpval){
						 throw new RuleEngineException(localizeErrorMessage(HourSetDescriptionProcessor.EMPVAL_UNASSIGNED,
								 HourSetDescriptionProcessor.EMPVAL_UNASSIGNED_default, empValOrUdfName));
					 }else{
						 throw new RuleEngineException(localizeErrorMessage(HourSetDescriptionProcessor.EMPUDF_UNASSIGNED, 
								 HourSetDescriptionProcessor.EMPUDF_UNASSIGNED_default, empValOrUdfName));
					 }
				 }
			}
		 
		 	if (!StringUtil.isEmpty(minStr)){
		 		try{
		 			doubleMinutes = Double.parseDouble(StringUtil.trimAll(minStr));
		 			if(doubleMinutes < 0){
		 				throw new RuleEngineException(localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_NEGATIVE_ERROR, HourSetDescriptionProcessor.HOURSET_NEGATIVE_ERROR_default, hourTypeName));
		 			}
		 			
		 			if (multExist){
		 				doubleMinutes = Math.round((doubleMinutes * multValue));
		 			}
		 		}
		 		catch (NumberFormatException ex) {
		 			throw new RuleEngineException(localizeErrorMessage(HourSetDescriptionProcessor.HOURSET_SYNTAX_ERROR, HourSetDescriptionProcessor.HOURSET_SYNTAX_ERROR_default));
		 		}
		 	}

		 return (int)doubleMinutes;
	 }
	
		static String localizeErrorMessage(String msgId, String defaultMsg, String arg){
			return ErrorMessageHelper.getMLString(msgId, defaultMsg, arg);
		}
		
		static String localizeErrorMessage(String msgId, String defaultMsg){
			return ErrorMessageHelper.getMLString(msgId, defaultMsg);
		}
	
}
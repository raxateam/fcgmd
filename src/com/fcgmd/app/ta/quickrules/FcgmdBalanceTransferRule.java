package com.fcgmd.app.ta.quickrules;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.workbrain.app.ta.model.BalanceData;
import com.workbrain.app.ta.quickrules.ConsolidatedWeeklyOvertimeRule;
import com.workbrain.app.ta.ruleengine.ImmutableRuleParameterInfo;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.RuleParameterInfoCache;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;

/**
 * Simple balance transfer rule based on core balance transfer rule. Here we are
 * using cap amount to transfer balance vs ratio in core.
 */
public class FcgmdBalanceTransferRule extends Rule {

	private static final long serialVersionUID = 1L;


	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FcgmdBalanceTransferRule.class);


	public static final String PARAM_BALANCE_TRANSFER_FROM = "BalanceTransferFrom";
	public static final String PARAM_BALANCE_TRANSFER_TO = "BalanceTransferTo";
	public static final String PARAM_BALANCE_TRANSFER_OVER_CAP = "BalanceTransferOverCap";
	public static final String PARAM_BALANCE_TRANSFER_MESSAGE = "BalanceTransferMessage";
	public static final String PARAM_BALANCE_TRANSFER_OR_DEDUCT = "BalanceTransferOrDeduct";
	public static final String PARAM_DEDUCT_BALANCE = "DeductBalance";
	public static final String PARAM_VAL_DFLT_MSG = "BalanceTransferRule";

	public static final String TRANSFER = "1";
	public static final String DEDUCT = "2";

	private static final List<ImmutableRuleParameterInfo> PARAMETER_INFO;

	static {       
		List<ImmutableRuleParameterInfo> paramList = new ArrayList<ImmutableRuleParameterInfo>();
		paramList.add(new ImmutableRuleParameterInfo(PARAM_BALANCE_TRANSFER_FROM, RuleParameterInfo.STRING_TYPE));
		paramList.add(new ImmutableRuleParameterInfo(PARAM_BALANCE_TRANSFER_TO, RuleParameterInfo.STRING_TYPE));
		paramList.add(new ImmutableRuleParameterInfo(PARAM_BALANCE_TRANSFER_OVER_CAP, RuleParameterInfo.STRING_TYPE));
		paramList.add(new ImmutableRuleParameterInfo(PARAM_BALANCE_TRANSFER_MESSAGE, RuleParameterInfo.STRING_TYPE));
		paramList.add(new ImmutableRuleParameterInfo(PARAM_BALANCE_TRANSFER_OR_DEDUCT, RuleParameterInfo.STRING_TYPE));
		paramList.add(new ImmutableRuleParameterInfo(PARAM_DEDUCT_BALANCE, RuleParameterInfo.STRING_TYPE));

		PARAMETER_INFO = Collections.unmodifiableList(paramList);
		RuleParameterInfoCache cache = RuleParameterInfoCache.getInstance();
		cache.addUnmodifiableRuleParameterInfoList(ConsolidatedWeeklyOvertimeRule.class.getName(), PARAMETER_INFO);  
	}   

	@Override
	public List<RuleParameterInfo> getParameterInfo (DBConnection conn) {
		ArrayList<RuleParameterInfo> result = new ArrayList<RuleParameterInfo>();
		result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_FROM, RuleParameterInfo.STRING_TYPE, false));
		result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_TO, RuleParameterInfo.STRING_TYPE, true));
		result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_OVER_CAP, RuleParameterInfo.STRING_TYPE, true));
		result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_MESSAGE, RuleParameterInfo.STRING_TYPE, true));
		result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_OR_DEDUCT, RuleParameterInfo.STRING_TYPE, false));
		return result;
	}

	@Override
	public void execute(WBData wbData, Parameters parameters) throws SQLException, Exception {
		BalanceData bdTo = new BalanceData();
		double transfer=0;
		boolean deduct = false;
		
		String tranOrDed = parameters.getParameter(PARAM_BALANCE_TRANSFER_OR_DEDUCT);
		if(tranOrDed.equals(TRANSFER)){
			deduct=false;
		}else if(tranOrDed.equals(DEDUCT)){
			deduct=true;
		}

		String balFrom = parameters.getParameter(PARAM_BALANCE_TRANSFER_FROM);
		BalanceData bdFrom = wbData.getCodeMapper().getBalanceForName(balFrom)   ;
		if (bdFrom == null) {
			throw new Exception("{ML}RD_BALANCE_NOT_FOUND{/ML}Balance not found for ID : " + balFrom + "{ARGS}" + balFrom + "{/ARGS}");
		}
		double fromBalVal = wbData.getEmployeeBalanceValue(bdFrom.getBalId());

		if(deduct==false){ 
			String balTo = parameters.getParameter(PARAM_BALANCE_TRANSFER_TO);
			bdTo = wbData.getCodeMapper().getBalanceForName(balTo)   ;
			if (bdTo == null) {
				throw new Exception("{ML}RD_BALANCE_NOT_FOUND{/ML}Balance not found for ID : " + bdTo + "{ARGS}" + bdTo + "{/ARGS}");
			}
			if (bdFrom.getBaltypId() != bdTo.getBaltypId()) {
				throw new RuntimeException ("{ML}BALANCE_TRANSFER_RULE_BAL_TYPE_MISMATCH{/ML} BalanceTransferRule: BalanceTransferFrom and BalanceTransferTo must have same unit types.");
			}
		}

		double capAmount = parameters.getDoubleParameter(PARAM_BALANCE_TRANSFER_OVER_CAP , 0.0d);
		String balMsg = parameters.getParameter(PARAM_BALANCE_TRANSFER_MESSAGE , PARAM_VAL_DFLT_MSG);

		if(fromBalVal != 0 && fromBalVal > capAmount ){
			transfer = fromBalVal - capAmount;
			wbData.addEmployeeBalanceValue(bdFrom.getBalId() , -1 * transfer, balMsg);
			if (logger.isDebugEnabled()) logger.debug("Subtracted :" + transfer + " for balance : " + bdFrom.getBalName());
			// *** do transfer if not deduct
			if (deduct==false) {
				wbData.addEmployeeBalanceValue(bdTo.getBalId() , transfer, balMsg);
				if (logger.isDebugEnabled()) logger.debug("Added :" + transfer + " for balance : " + bdTo.getBalName());
			}
		}
	}

	@Override
	public String getComponentName() {
		return "FCGMD Balance Transfer Rule";
	}

	@Override
	public String getComponentUI() {
		return "/quickrules/qFCGMDBalanceTransferParams.jsp";
	}

}

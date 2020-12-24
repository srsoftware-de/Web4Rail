package de.srsoftware.web4rail.conditions;

public class AndCondition extends ConditionList{
	
	@Override
	public boolean fulfilledBy(Context context) {
		for (Condition condition : this) {
			if (!condition.fulfilledBy(context)) return false;
		}
		return true;
	}
	
	@Override
	protected String glue() {
		return t("and");
	}	
}

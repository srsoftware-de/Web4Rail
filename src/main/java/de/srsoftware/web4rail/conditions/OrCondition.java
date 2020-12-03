package de.srsoftware.web4rail.conditions;

public class OrCondition extends ConditionList{
	
	@Override
	public boolean fulfilledBy(Context context) {
		for (Condition condition : this) {
			if (condition.fulfilledBy(context)) return true;
		}
		return false;
	}
	
	@Override
	protected String glue() {
		return t("or");
	}	
}

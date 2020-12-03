package de.srsoftware.web4rail.conditions;

import java.util.stream.Collectors;

public class OrCondition extends ConditionList{
	
	@Override
	public boolean fulfilledBy(Context context) {
		for (Condition condition : this) {
			if (condition.fulfilledBy(context)) return true;
		}
		return false;
	}
		
	@Override
	public String toString() {
		return isEmpty() ? t("Click here to select conditions") : String.join(" "+t("OR")+" ", stream().map(Object::toString).collect(Collectors.toList()));
	}
}

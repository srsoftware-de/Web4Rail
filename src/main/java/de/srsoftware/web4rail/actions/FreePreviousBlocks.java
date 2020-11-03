package de.srsoftware.web4rail.actions;

import java.io.IOException;

public class FreePreviousBlocks extends Action {

	@Override
	public boolean fire(Context context) throws IOException {
		if (context.train != null) context.train.resetPreviousBlocks();
		return false;
	}
}

package de.srsoftware.web4rail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Block;

public class Destination {
	private static final Logger LOG = LoggerFactory.getLogger(Destination.class);

	private Direction enterDirection;
	public Block block;

	public Destination(Block block, Direction enterFrom) {
		this.block = block;
		this.enterDirection = enterFrom;
	}
	
	public Destination(Block block) {
		this(block,null);
	}
	
	@SuppressWarnings("unused")
	private Destination() {}

	boolean accepts(Direction enterDirection) {
		boolean result = this.enterDirection == null || enterDirection == null || this.enterDirection == enterDirection;
		LOG.debug(BaseClass.t(result ? "{} accepts train from {}" : "{} does not accept train from {}",this,enterDirection.inverse()));
		return result;
	}
	
	public String block() {
		return block.id().toString();
	};
	
	@Override
	public String toString() {
		return enterDirection == null ? block.toString() : BaseClass.t("{} from {}",block,enterDirection.inverse());
	}


}
